package off.kys.textgrab.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.isRtl
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hybrid OCR Engine. Uses Google ML Kit for fast Latin text recognition and
 * Tesseract OCR for robust offline Arabic support.
 *
 * Owns native resources (ML Kit recognizer, Tesseract engine) for its whole
 * lifetime — call [close] when the owner (e.g. a ViewModel) is torn down.
 */
class OcrEngine {

    private val latin: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val nextId = AtomicLong(0)
    private val tessMutex = Mutex()
    private var tess: TessBaseAPI? = null

    suspend fun recognize(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage = OcrLanguage.BOTH
    ): List<GrabbedText> = withContext(Dispatchers.Default) {
        val safeBitmap = bitmap.toSoftwareBitmapIfNeeded()
        val image = InputImage.fromBitmap(safeBitmap, 0)

        // Run both recognizers concurrently; they are independent.
        val latinDeferred = if (language == OcrLanguage.LATIN || language == OcrLanguage.BOTH) {
            async { runCatching { latin.process(image).await() }.getOrNull() }
        } else null

        val arabicDeferred = if (language == OcrLanguage.ARABIC || language == OcrLanguage.BOTH) {
            async { recognizeArabic(safeBitmap, context, language) }
        } else null

        val latinResult = latinDeferred?.await()
        val arabicResult = arabicDeferred?.await().orEmpty()

        if (safeBitmap !== bitmap) safeBitmap.recycle()

        val lines = ArrayList<GrabbedText>()

        // Collect Latin results
        latinResult?.let { result ->
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val text = line.text.trim()

                    if (text.isEmpty()) continue
                    if (language == OcrLanguage.BOTH && text.arabicRatio() > 0.5f) continue

                    lines += GrabbedText(
                        id = nextId.getAndIncrement(),
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom,
                        source = ExtractionMode.OCR,
                        isRtl = text.isRtl() && text.arabicRatio() > 0.5f,
                    )
                }
            }
        }

        // Add Arabic results
        lines.addAll(arabicResult)

        dedupe(lines).sortedWith(compareBy({ it.top }, { it.left }))
    }

    /** Releases native resources. Call when the owner of this engine is destroyed. */
    suspend fun close() {
        latin.close()
        tessMutex.withLock {
            tess?.recycle()
            tess = null
        }
    }

    /** Lazily creates (once) and reuses the Tesseract engine across calls. */
    private fun getOrInitTess(context: Context): TessBaseAPI? {
        tess?.let { return it }

        val newTess = TessBaseAPI()
        val dataPath = TessDataStore.getTessDataPath(context)

        // Use LSTM engine for better accuracy with modern .traineddata files.
        if (!newTess.init(dataPath, "ara", TessBaseAPI.OEM_LSTM_ONLY)) {
            Log.e("OcrEngine", "Tesseract failed to initialize with 'ara' at $dataPath")
            newTess.recycle()
            return null
        }
        tess = newTess
        return newTess
    }

    private suspend fun recognizeArabic(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage,
    ): List<GrabbedText> = tessMutex.withLock {
        var prepared: Bitmap? = null
        try {
            val api = getOrInitTess(context) ?: return@withLock emptyList()

            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO

            // Unlike ML Kit, Tesseract cannot cope with light-on-dark UI themes:
            // grayscale the frame and invert it when the screen is predominantly dark.
            prepared = preprocess(bitmap)
            api.setImage(prepared)

            // setImage() alone does not run recognition: getUTF8Text() triggers the
            // (blocking) Recognize() pass that the resultIterator reads from.
            api.utF8Text

            val results = mutableListOf<GrabbedText>()
            val it = api.resultIterator
            if (it == null) {
                Log.e("OcrEngine", "Tesseract returned no result iterator")
                return@withLock emptyList()
            }

            it.begin()
            do {
                val text = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)?.trim()
                if (!text.isNullOrEmpty()) {
                    val confidence = it.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)

                    // Drop low-confidence lines: they are almost always Latin regions
                    // misread through the Arabic model.
                    if (confidence >= MIN_TESS_CONFIDENCE) {
                        // In hybrid mode ML Kit owns Latin: a "detected" line without
                        // a single Arabic character is gibberish, not real text.
                        if (!(language == OcrLanguage.BOTH && text.arabicRatio() == 0f)) {
                            val box = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                            results += GrabbedText(
                                id = nextId.getAndIncrement(),
                                text = text,
                                left = box.left,
                                top = box.top,
                                right = box.right,
                                bottom = box.bottom,
                                source = ExtractionMode.OCR,
                                isRtl = text.isRtl(),
                            )
                        }
                    }
                }
            } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))

            results
        } catch (e: Exception) {
            Log.e("OcrEngine", "Tesseract error", e)
            emptyList()
        } finally {
            prepared?.recycle()
        }
    }

    /**
     * Grayscale the frame and, if the screen is predominantly dark (night theme),
     * invert it so Tesseract sees dark text on a light background — the only
     * polarity its binarization handles reliably.
     */
    private fun preprocess(src: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        if (src.meanLuminance() < DARK_SCREEN_LUMINANCE) {
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )
        }
        val out = createBitmap(src.width, src.height)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Mean luminance (0-255) estimated from a 32x32 downsample of the frame. */
    private fun Bitmap.meanLuminance(): Float {
        val sample = this.scale(32, 32)
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (sample !== this) sample.recycle()
        var sum = 0L
        for (c in pixels) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sum += (r * 299 + g * 587 + b * 114) / 1000
        }
        return sum.toFloat() / pixels.size
    }

    /**
     * ML Kit and Bitmap.getPixels() both require a non-hardware config.
     * Screen-capture sources (PixelCopy / MediaProjection) frequently hand back
     * Config.HARDWARE, which throws IllegalStateException on either call site.
     * Returns the original bitmap unchanged when no conversion is needed.
     */
    private fun Bitmap.toSoftwareBitmapIfNeeded(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) {
            this.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            this
        }

    /**
     * Both recognizers frequently detect the same physical line. Keep the longer
     * text when two boxes overlap substantially (the correct-script recognizer
     * usually yields the fuller, more accurate string).
     */
    private fun dedupe(items: List<GrabbedText>): List<GrabbedText> {
        val kept = ArrayList<GrabbedText>()
        for (item in items.sortedByDescending { it.text.length }) {
            if (kept.none { overlapRatio(it, item) > OVERLAP_THRESHOLD }) {
                kept += item
            }
        }
        return kept
    }

    private fun overlapRatio(a: GrabbedText, b: GrabbedText): Float {
        val interW = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0)
        val interH = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0)
        val intersection = interW.toFloat() * interH.toFloat()
        val minArea = minOf(
            (a.width * a.height).toFloat(),
            (b.width * b.height).toFloat(),
        ).coerceAtLeast(1f)
        return intersection / minArea
    }

    private companion object {
        const val OVERLAP_THRESHOLD = 0.6f

        /** Tesseract line confidence (0-100) below which a line is considered noise. */
        const val MIN_TESS_CONFIDENCE = 50f

        /** Mean luminance below which the frame is treated as a dark theme and inverted. */
        const val DARK_SCREEN_LUMINANCE = 110f
    }
}

/**
 * Fraction of letters that belong to the Arabic script. Used to decide which
 * engine "owns" a line: mostly-Arabic lines go to Tesseract, mostly-Latin ones
 * to ML Kit. Non-letters (digits, punctuation) are ignored.
 */
private fun String.arabicRatio(): Float {
    var letters = 0
    var arabic = 0
    for (ch in this) {
        if (!ch.isLetter()) continue
        letters++
        val code = ch.code
        val isArabic = code in 0x0600..0x06FF ||   // Arabic
                code in 0x0750..0x077F ||              // Arabic Supplement
                code in 0x08A0..0x08FF ||              // Arabic Extended-A
                code in 0xFB50..0xFDFF ||              // Arabic Presentation Forms-A
                code in 0xFE70..0xFEFF                 // Arabic Presentation Forms-B
        if (isArabic) arabic++
    }
    return if (letters == 0) 0f else arabic.toFloat() / letters
}

/** Bridges a Play-services [Task] into a coroutine without the extra dependency. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
    addOnCanceledListener { cont.cancel() }
}
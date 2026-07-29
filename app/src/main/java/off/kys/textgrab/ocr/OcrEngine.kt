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
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.isRtl
import java.util.concurrent.atomic.AtomicLong

/**
 * Tesseract-based OCR engine with fully offline Latin ("eng") and Arabic ("ara")
 * support. [OcrLanguage.BOTH] runs a single "ara+eng" pass so Tesseract picks the
 * best script per word.
 *
 * Owns the native Tesseract engine for its whole lifetime — call [close] when the
 * owner (e.g. a service) is torn down.
 */
class OcrEngine {

    private val nextId = AtomicLong(0)
    private val tessMutex = Mutex()
    private var tess: TessBaseAPI? = null
    private var tessLanguage: String? = null

    suspend fun recognize(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage = OcrLanguage.BOTH
    ): List<GrabbedText> = withContext(Dispatchers.Default) {
        val safeBitmap = bitmap.toSoftwareBitmapIfNeeded()
        val lines = runTesseract(safeBitmap, context, language)
        if (safeBitmap !== bitmap) safeBitmap.recycle()

        lines.sortedWith(compareBy({ it.top }, { it.left }))
    }

    /**
     * Pre-warms the Tesseract engine for the given language on a background thread.
     * This ensures that data files are copied and the native engine is initialized
     * before the first recognition request.
     */
    suspend fun prepare(context: Context, language: OcrLanguage) = withContext(Dispatchers.Default) {
        tessMutex.withLock {
            getOrInitTess(context, language.toTessLanguage())
        }
    }

    /** Releases native resources. Call when the owner of this engine is destroyed. */
    suspend fun close() {
        tessMutex.withLock {
            tess?.recycle()
            tess = null
            tessLanguage = null
        }
    }

    /**
     * Lazily creates and reuses the Tesseract engine across calls. Re-initializes
     * only when the requested language string differs from the current one.
     * Must be called while holding [tessMutex].
     */
    private suspend fun getOrInitTess(context: Context, tessLang: String): TessBaseAPI? = withContext(Dispatchers.IO) {
        tess?.let { existing ->
            if (tessLanguage == tessLang) return@withContext existing
            existing.recycle()
            tess = null
            tessLanguage = null
        }

        val newTess = TessBaseAPI()
        val dataPath = TessDataStore.getTessDataPath(context, tessLang)

        // Use LSTM engine for better accuracy with modern .traineddata files.
        if (!newTess.init(dataPath, tessLang, TessBaseAPI.OEM_LSTM_ONLY)) {
            Log.e("OcrEngine", "Tesseract failed to initialize with '$tessLang' at $dataPath")
            newTess.recycle()
            return@withContext null
        }
        tess = newTess
        tessLanguage = tessLang
        newTess
    }

    private suspend fun runTesseract(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage,
    ): List<GrabbedText> = tessMutex.withLock {
        var prepared: Bitmap? = null
        try {
            val initStart = System.currentTimeMillis()
            val api = getOrInitTess(context, language.toTessLanguage())
                ?: return@withLock emptyList()
            Log.d("OcrEngine", "DIAG init/reuse '${language.toTessLanguage()}' took ${System.currentTimeMillis() - initStart}ms")

            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO

            // Tesseract cannot cope with light-on-dark UI themes: grayscale the
            // frame and invert it when the screen is predominantly dark.
            val lum = bitmap.meanLuminance()
            Log.d("OcrEngine", "DIAG src ${bitmap.width}x${bitmap.height} cfg=${bitmap.config} luminance=$lum inverted=${lum < DARK_SCREEN_LUMINANCE}")
            prepared = preprocess(bitmap)
            // DIAG: dump the exact image Tesseract sees so it can be pulled via adb.
            runCatching {
                java.io.File(context.filesDir, "ocr_debug.png").outputStream().use {
                    prepared.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            api.setImage(prepared)

            // setImage() alone does not run recognition: getUTF8Text() triggers the
            // (blocking) Recognize() pass that the resultIterator reads from.
            val recogStart = System.currentTimeMillis()
            val fullText = api.utF8Text
            Log.d("OcrEngine", "DIAG recognize took ${System.currentTimeMillis() - recogStart}ms, fullText.length=${fullText?.length}, meanConf=${api.meanConfidence()}")

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
                    Log.d("OcrEngine", "DIAG line conf=$confidence text='${text.take(40)}'")

                    // Drop low-confidence lines: they are almost always noise
                    // (icons, gradients, anti-aliased edges) misread as text.
                    if (confidence >= MIN_TESS_CONFIDENCE) {
                        val box = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                        results += GrabbedText(
                            id = nextId.getAndIncrement(),
                            text = text,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom,
                            source = ExtractionMode.OCR,
                            // Only treat mostly-Arabic lines as RTL: a stray Arabic
                            // character inside a Latin line must not flip the layout.
                            isRtl = text.isRtl() && text.arabicRatio() > 0.5f,
                        )
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
     * Tesseract's setImage() and Bitmap.getPixels() both require a non-hardware
     * config. Screen-capture sources (PixelCopy / MediaProjection) frequently hand
     * back Config.HARDWARE, which throws IllegalStateException on either call site.
     * Returns the original bitmap unchanged when no conversion is needed.
     */
    private fun Bitmap.toSoftwareBitmapIfNeeded(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) {
            this.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            this
        }

    private companion object {
        /** Tesseract line confidence (0-100) below which a line is considered noise. */
        const val MIN_TESS_CONFIDENCE = 50f

        /** Mean luminance below which the frame is treated as a dark theme and inverted. */
        const val DARK_SCREEN_LUMINANCE = 110f
    }
}

/** Maps the user-facing language selection to a Tesseract language string. */
private fun OcrLanguage.toTessLanguage(): String = when (this) {
    OcrLanguage.LATIN -> "eng"
    OcrLanguage.ARABIC -> "ara"
    OcrLanguage.BOTH -> "ara+eng"
}

/**
 * Fraction of letters that belong to the Arabic script. Used to decide whether a
 * recognized line should be laid out right-to-left. Non-letters (digits,
 * punctuation) are ignored.
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

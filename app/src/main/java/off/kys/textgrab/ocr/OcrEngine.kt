package off.kys.textgrab.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
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
 */
class OcrEngine {

    private val latin: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val nextId = AtomicLong(0)

    suspend fun recognize(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage = OcrLanguage.BOTH
    ): List<GrabbedText> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // Run ML Kit for Latin
        val latinResult = if (language == OcrLanguage.LATIN || language == OcrLanguage.BOTH) {
            runCatching { latin.process(image).await() }.getOrNull()
        } else null
        
        // Run Tesseract for Arabic
        val arabicResult = if (language == OcrLanguage.ARABIC || language == OcrLanguage.BOTH) {
            recognizeArabic(bitmap, context)
        } else emptyList()

        val lines = ArrayList<GrabbedText>()

        // Collect Latin results
        latinResult?.let { result ->
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val text = line.text.trim()
                    if (text.isEmpty()) continue
                    
                    // If we are in hybrid mode, skip Latin results that are strongly Arabic
                    // (ML Kit Latin sometimes produces gibberish for Arabic script).
                    if (language == OcrLanguage.BOTH && text.isRtl()) continue

                    lines += GrabbedText(
                        id = nextId.getAndIncrement(),
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom,
                        source = ExtractionMode.OCR,
                        isRtl = false, // Latin engine results are LTR
                    )
                }
            }
        }

        // Add Arabic results
        lines.addAll(arabicResult)

        dedupe(lines).sortedWith(compareBy({ it.top }, { it.left }))
    }

    private fun recognizeArabic(bitmap: Bitmap, context: Context): List<GrabbedText> {
        val tess = TessBaseAPI()
        return try {
            val dataPath = TessDataStore.getTessDataPath(context)
            
            // Use LSTM engine for better accuracy with modern .traineddata files.
            // PageSegMode.PSM_AUTO allows Tesseract to find text blocks and lines.
            tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            
            if (!tess.init(dataPath, "ara", TessBaseAPI.OEM_LSTM_ONLY)) {
                Log.e("OcrEngine", "Tesseract failed to initialize with 'ara' at $dataPath")
                return emptyList()
            }

            tess.setImage(bitmap)
            
            val results = mutableListOf<GrabbedText>()
            val it = tess.resultIterator ?: return emptyList()
            
            it.begin()
            do {
                val text = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)?.trim()
                if (!text.isNullOrEmpty()) {
                    val box = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                    results += GrabbedText(
                        id = nextId.getAndIncrement(),
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom,
                        source = ExtractionMode.OCR,
                        isRtl = true
                    )
                }
            } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
            
            results
        } catch (e: Exception) {
            Log.e("OcrEngine", "Tesseract error", e)
            emptyList()
        } finally {
            tess.recycle()
        }
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
    }
}

/** Bridges a Play-services [Task] into a coroutine without the extra dependency. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
    addOnCanceledListener { cont.cancel() }
}

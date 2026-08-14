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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.isRtl
import off.kys.textgrab.ocr.model.TesseractVersion
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Tesseract-based OCR engine.
 */
class OcrEngine(private val repository: OcrPackageRepository) {

    private val nextId = AtomicLong(0)
    private val tessMutex = Mutex()
    private var tess: TessBaseAPI? = null
    private var tessLanguage: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var defaultsJob: Job? = null
    private val currentDefaults = MutableStateFlow<Map<String, TesseractVersion>>(emptyMap())

    init {
        defaultsJob = scope.launch {
            repository.defaultVersions.collect {
                currentDefaults.value = it
            }
        }
    }

    suspend fun isLoaded(language: OcrLanguage): Boolean {
        val tessLang = resolveTessLanguage(language)
        val codes = tessLang.split('+')
        val defaults = currentDefaults.value

        return codes.all { code ->
            val ver = defaults[code] ?: repository.getDefaultVersion(code)
            repository.isInstalled(code, ver)
        }
    }

    suspend fun recognize(
        bitmap: Bitmap,
        context: Context,
        language: OcrLanguage = OcrLanguage.LATIN
    ): List<GrabbedText> = withContext(Dispatchers.Default) {
        if (!isLoaded(language)) {
            Log.e("OcrEngine", "OCR language '$language' is not fully installed.")
            return@withContext emptyList()
        }

        val safeBitmap = bitmap.toSoftwareBitmapIfNeeded()
        val tessLang = resolveTessLanguage(language)
        val lines = runTesseract(safeBitmap, context, tessLang)
        if (safeBitmap !== bitmap) safeBitmap.recycle()

        lines.sortedWith(compareBy({ it.top }, { it.left }))
    }

    suspend fun prepare(context: Context, language: OcrLanguage) = withContext(Dispatchers.Default) {
        tessMutex.withLock {
            val tessLang = resolveTessLanguage(language)
            getOrInitTess(context, tessLang)
        }
    }

    suspend fun close() {
        defaultsJob?.cancel()
        scope.cancel()
        tessMutex.withLock {
            tess?.recycle()
            tess = null
            tessLanguage = null
        }
    }

    private fun resolveTessLanguage(language: OcrLanguage): String = when (language) {
        OcrLanguage.LATIN -> "eng"
        OcrLanguage.ARABIC -> "ara"
        OcrLanguage.FRENCH -> "fra"
        OcrLanguage.GERMAN -> "deu"
        OcrLanguage.CHINESE -> "chi_sim"
        OcrLanguage.JAPANESE -> "jpn"
        OcrLanguage.KOREAN -> "kor"
        OcrLanguage.AUTO -> {
            val installed = repository.getAvailablePackages().filter { pkg ->
                TesseractVersion.entries.any { ver -> repository.isInstalled(pkg.tessCode, ver) }
            }.map { it.tessCode }
            if (installed.isEmpty()) "eng" else installed.joinToString("+")
        }
    }

    private suspend fun getOrInitTess(context: Context, tessLang: String): TessBaseAPI? = withContext(Dispatchers.IO) {
        if (tess != null && tessLanguage == tessLang) return@withContext tess

        tess?.recycle()
        tess = null
        tessLanguage = null

        val newTess = TessBaseAPI()
        val dataPath = prepareDataPath(context, tessLang)

        if (!newTess.init(dataPath, tessLang, TessBaseAPI.OEM_LSTM_ONLY)) {
            Log.e("OcrEngine", "Tesseract failed to initialize with '$tessLang' at $dataPath")
            newTess.recycle()
            return@withContext null
        }
        tess = newTess
        tessLanguage = tessLang
        newTess
    }

    private suspend fun prepareDataPath(context: Context, tessLang: String): String = withContext(Dispatchers.IO) {
        val codes = tessLang.split('+')
        val defaults = currentDefaults.value

        if (codes.size == 1) {
            val ver = defaults[codes[0]] ?: repository.getDefaultVersion(codes[0])
            return@withContext TessDataStore.getTessDataPath(context, ver)
        }

        // For multiple languages, merge them into a single directory
        val mergedDir = File(context.filesDir, "ocr_data/merged/tessdata")
        if (!mergedDir.exists()) mergedDir.mkdirs()

        for (code in codes) {
            val ver = defaults[code] ?: repository.getDefaultVersion(code)
            val source = File(TessDataStore.getTessDataPath(context, ver), "tessdata/$code.traineddata")
            val dest = File(mergedDir, "$code.traineddata")
            if (source.exists()) {
                if (!dest.exists() || dest.lastModified() < source.lastModified()) {
                    source.copyTo(dest, overwrite = true)
                }
            }
        }
        File(context.filesDir, "ocr_data/merged").absolutePath
    }

    private suspend fun runTesseract(
        bitmap: Bitmap,
        context: Context,
        tessLang: String,
    ): List<GrabbedText> = tessMutex.withLock {
        var prepared: Bitmap? = null
        try {
            val api = getOrInitTess(context, tessLang) ?: return@withLock emptyList()
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            api.setVariable("user_defined_dpi", "300")

            prepared = preprocess(bitmap)
            api.setImage(prepared)

            // IMPORTANT: setImage() only loads the image; it does NOT run recognition.
            // This library's Java API has no public recognize() method — recognition is
            // triggered implicitly the first time a get*Text() call is made. resultIterator
            // on its own returns an iterator over nothing if recognition never ran, so we
            // force it here by calling getUTF8Text() once before iterating.
            val fullText = api.utF8Text
            if (fullText.isNullOrEmpty()) {
                return@withLock emptyList()
            }

            val results = mutableListOf<GrabbedText>()
            val it = api.resultIterator ?: return@withLock emptyList()

            it.begin()
            do {
                val text = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)?.trim()
                if (!text.isNullOrEmpty()) {
                    val confidence = it.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
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

    private fun preprocess(src: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply { setSaturation(0f) }

        // Increase contrast: scale color values and then shift them
        val contrast = 1.4f
        val translate = (-.5f * contrast + .5f) * 255f
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

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

    private fun Bitmap.toSoftwareBitmapIfNeeded(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) {
            this.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            this
        }

    private companion object {
        const val MIN_TESS_CONFIDENCE = 50f
        const val DARK_SCREEN_LUMINANCE = 110f
    }
}

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
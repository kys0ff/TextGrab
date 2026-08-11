package off.kys.textgrab.ocr

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion

class OcrPackageRepository(private val context: Context) {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    fun updateDownloadState(key: String, state: DownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[key] = state
        _downloadStates.value = current
    }

    fun getAvailablePackages(): List<OcrPackage> {
        val recommended = getRecommendedVersion()
        return listOf(
            createPackage(OcrLanguage.LATIN, "English", "eng", recommended),
            createPackage(OcrLanguage.ARABIC, "Arabic", "ara", recommended),
            createPackage(OcrLanguage.FRENCH, "French", "fra", recommended),
            createPackage(OcrLanguage.GERMAN, "German", "deu", recommended),
            createPackage(OcrLanguage.CHINESE, "Chinese (Simplified)", "chi_sim", recommended),
            createPackage(OcrLanguage.JAPANESE, "Japanese", "jpn", recommended),
            createPackage(OcrLanguage.KOREAN, "Korean", "kor", recommended)
        )
    }

    private fun createPackage(lang: OcrLanguage, name: String, code: String, recommended: TesseractVersion): OcrPackage {
        // Approximate sizes based on Tesseract repo defaults
        val baseSize = when(code) {
            "chi_sim", "jpn", "kor" -> 40_000_000L
            "ara" -> 12_000_000L
            else -> 15_000_000L
        }

        return OcrPackage(
            language = lang,
            displayName = name,
            tessCode = code,
            versions = listOf(
                OcrVersion(TesseractVersion.FAST, "https://github.com/tesseract-ocr/tessdata_fast/raw/main/$code.traineddata", baseSize / 4, recommended == TesseractVersion.FAST),
                OcrVersion(TesseractVersion.STANDARD, "https://github.com/tesseract-ocr/tessdata/raw/main/$code.traineddata", baseSize, recommended == TesseractVersion.STANDARD),
                OcrVersion(TesseractVersion.BEST, "https://github.com/tesseract-ocr/tessdata_best/raw/main/$code.traineddata", baseSize * 2, recommended == TesseractVersion.BEST)
            )
        )
    }

    private fun getRecommendedVersion(): TesseractVersion {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024f)
        val cores = Runtime.getRuntime().availableProcessors()
        val isLowEnd = activityManager.isLowRamDevice
        
        return when {
            !isLowEnd && totalRamGb >= 6 && cores >= 8 -> TesseractVersion.BEST
            !isLowEnd && totalRamGb >= 3 && cores >= 4 -> TesseractVersion.STANDARD
            else -> TesseractVersion.FAST
        }
    }

    fun isInstalled(tessCode: String, version: TesseractVersion): Boolean {
        return TessDataStore.isInstalled(context, tessCode, version)
    }

    fun refreshInstallationStates() {
        val states = mutableMapOf<String, DownloadState>()
        getAvailablePackages().forEach { pkg ->
            pkg.versions.forEach { ver ->
                val key = "${pkg.tessCode}_${ver.version}"
                if (isInstalled(pkg.tessCode, ver.version)) {
                    states[key] = DownloadState.Downloaded
                } else if (_downloadStates.value[key] !is DownloadState.Downloading) {
                    states[key] = DownloadState.NotDownloaded
                } else {
                    states[key] = _downloadStates.value[key]!!
                }
            }
        }
        _downloadStates.value = states
    }
}

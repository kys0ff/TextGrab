package off.kys.textgrab.ocr.model

import off.kys.textgrab.core.model.OcrLanguage

/**
 * Tesseract engine variants.
 * - [FAST]: Integerized, smaller, optimized for speed.
 * - [STANDARD]: Default models.
 * - [BEST]: Float models, highest accuracy, slower.
 */
enum class TesseractVersion {
    FAST, STANDARD, BEST
}

data class OcrPackage(
    val language: OcrLanguage,
    val displayName: String,
    val tessCode: String,
    val versions: List<OcrVersion>
)

data class OcrVersion(
    val version: TesseractVersion,
    val url: String,
    val sizeBytes: Long,
    val isRecommended: Boolean = false,
    val downloadState: DownloadState = DownloadState.NotDownloaded,
    val isDefault: Boolean = false
)

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
    data class Error(val message: String) : DownloadState
}

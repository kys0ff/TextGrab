package off.kys.textgrab.ui.screens.ocr

import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.TesseractVersion

sealed interface OcrPackageEvent {
    data class Download(val pkg: OcrPackage, val version: TesseractVersion, val url: String) : OcrPackageEvent
    data class Delete(val pkg: OcrPackage, val version: TesseractVersion) : OcrPackageEvent
    data class ConfirmDelete(val pkg: OcrPackage, val version: TesseractVersion) : OcrPackageEvent
    data object DismissDeleteDialog : OcrPackageEvent
    data class SetDefault(val pkg: OcrPackage, val version: TesseractVersion) : OcrPackageEvent
    data object Refresh : OcrPackageEvent
}
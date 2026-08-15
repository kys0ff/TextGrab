package off.kys.textgrab.ui.screens.ocr

import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.TesseractVersion

data class OcrPackageState(
    val packages: List<OcrPackage> = emptyList(),
    val deleteConfirmation: DeleteConfirmation? = null
)

data class DeleteConfirmation(
    val pkg: OcrPackage,
    val version: TesseractVersion
)
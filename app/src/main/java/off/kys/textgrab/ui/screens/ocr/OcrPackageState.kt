package off.kys.textgrab.ui.screens.ocr

import off.kys.textgrab.ocr.model.OcrPackage

data class OcrPackageState(
    val packages: List<OcrPackage> = emptyList()
)
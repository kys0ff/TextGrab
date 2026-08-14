package off.kys.textgrab.overlay.ui

import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.OverlayStatus

data class OverlayUiState(
    val elements: List<GrabbedText> = emptyList(),
    val mode: ExtractionMode = ExtractionMode.ACCESSIBILITY,
    val ocrLanguage: OcrLanguage = OcrLanguage.LATIN,
    val status: OverlayStatus = OverlayStatus.Idle,
    val isScrollMode: Boolean = false,
    val multiSelect: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isExpanded: Boolean = true,
    val missingLanguageDialog: OcrLanguage? = null,
    val autoModeWarningDialog: Boolean = false
)

sealed interface OverlayUiEvent {
    data object ToggleMultiSelect : OverlayUiEvent
    data class ToggleElementSelection(val id: Long) : OverlayUiEvent
    data object ClearSelection : OverlayUiEvent
    data object CopySelected : OverlayUiEvent
    data object CopyAll : OverlayUiEvent
    data class SwitchMode(val mode: ExtractionMode) : OverlayUiEvent
    data class SwitchLanguage(val language: OcrLanguage) : OverlayUiEvent
    data object Rescan : OverlayUiEvent
    data object Close : OverlayUiEvent
    data object OpenDownload : OverlayUiEvent
    data object ToggleHeaderExpansion : OverlayUiEvent
    data class SetScrollMode(val isScrollMode: Boolean) : OverlayUiEvent
    data object DismissMissingLanguageDialog : OverlayUiEvent
    data object DismissAutoModeWarningDialog : OverlayUiEvent
    data object ConfirmAutoModeWarning : OverlayUiEvent
}

sealed interface OverlayUiEffect {
    data class CopyText(val texts: List<String>, val mode: ExtractionMode) : OverlayUiEffect
    data object CloseOverlay : OverlayUiEffect
    data object NavigateToDownloads : OverlayUiEffect
}

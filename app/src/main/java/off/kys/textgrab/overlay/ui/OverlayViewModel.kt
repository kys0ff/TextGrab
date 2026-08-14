package off.kys.textgrab.overlay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.overlay.OverlayBus

class OverlayViewModel : ViewModel() {

    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OverlayUiEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    init {
        combine(
            OverlayBus.elements,
            OverlayBus.mode,
            OverlayBus.ocrLanguage,
            OverlayBus.status,
            OverlayBus.isScrollMode
        ) { elements, mode, ocrLanguage, status, isScrollMode ->
            _state.update {
                it.copy(
                    elements = elements,
                    mode = mode,
                    ocrLanguage = ocrLanguage,
                    status = status,
                    isScrollMode = isScrollMode
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: OverlayUiEvent) {
        when (event) {
            OverlayUiEvent.ToggleMultiSelect -> {
                _state.update { it.copy(multiSelect = !it.multiSelect, selectedIds = emptySet()) }
            }

            is OverlayUiEvent.ToggleElementSelection -> {
                _state.update {
                    val newSelected = if (it.selectedIds.contains(event.id)) {
                        it.selectedIds - event.id
                    } else {
                        it.selectedIds + event.id
                    }
                    it.copy(
                        selectedIds = newSelected,
                        multiSelect = if (newSelected.isNotEmpty()) true else it.multiSelect
                    )
                }
            }

            OverlayUiEvent.ClearSelection -> {
                _state.update { it.copy(selectedIds = emptySet(), multiSelect = false) }
            }

            OverlayUiEvent.CopySelected -> {
                val selectedTexts = _state.value.elements
                    .filter { _state.value.selectedIds.contains(it.id) }
                    .map { it.text }
                if (selectedTexts.isNotEmpty()) {
                    _effects.tryEmit(OverlayUiEffect.CopyText(selectedTexts, _state.value.mode))
                }
            }

            OverlayUiEvent.CopyAll -> {
                val allTexts = _state.value.elements.map { it.text }
                if (allTexts.isNotEmpty()) {
                    _effects.tryEmit(OverlayUiEffect.CopyText(allTexts, _state.value.mode))
                }
            }

            is OverlayUiEvent.SwitchMode -> {
                OverlayBus.send(OverlayCommand.SwitchMode(event.mode))
            }

            is OverlayUiEvent.SwitchLanguage -> {
                OverlayBus.ocrLanguage.value = event.language
                OverlayBus.send(OverlayCommand.Rescan)
            }

            OverlayUiEvent.Rescan -> {
                OverlayBus.send(OverlayCommand.Rescan)
            }

            OverlayUiEvent.Close -> {
                _effects.tryEmit(OverlayUiEffect.CloseOverlay)
            }

            OverlayUiEvent.OpenDownload -> {
                _effects.tryEmit(OverlayUiEffect.NavigateToDownloads)
            }

            OverlayUiEvent.ToggleHeaderExpansion -> {
                _state.update { it.copy(isExpanded = !it.isExpanded) }
            }

            is OverlayUiEvent.SetScrollMode -> {
                OverlayBus.send(OverlayCommand.SetScrollMode(event.isScrollMode))
            }

            OverlayUiEvent.DismissMissingLanguageDialog -> {
                _state.update { it.copy(missingLanguageDialog = null) }
            }

            OverlayUiEvent.DismissAutoModeWarningDialog -> {
                _state.update { it.copy(autoModeWarningDialog = false) }
            }

            OverlayUiEvent.ConfirmAutoModeWarning -> {
                _state.update { it.copy(autoModeWarningDialog = false) }
                OverlayBus.ocrLanguage.value = OcrLanguage.AUTO
                OverlayBus.send(OverlayCommand.Rescan)
            }
        }
    }

    fun showMissingLanguageDialog(language: OcrLanguage) {
        _state.update { it.copy(missingLanguageDialog = language) }
    }

    fun showAutoModeWarningDialog() {
        _state.update { it.copy(autoModeWarningDialog = true) }
    }
}

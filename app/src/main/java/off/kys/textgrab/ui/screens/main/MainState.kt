package off.kys.textgrab.ui.screens.main

import off.kys.textgrab.core.model.HistoryEntry

data class MainState(
    val history: List<HistoryEntry> = emptyList(),
    val permissions: PermissionUiState = PermissionUiState(),
    val showClearHistoryConfirmation: Boolean = false
)

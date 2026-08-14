package off.kys.textgrab.ui.screens.main

data class PermissionUiState(
    val accessibility: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
)
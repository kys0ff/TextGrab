package off.kys.textgrab.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import off.kys.textgrab.ServiceLocator
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.core.permission.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/** Snapshot of the three setup permissions, re-read every time the screen resumes. */
data class PermissionUiState(
    val accessibility: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
)

class MainViewModel : ViewModel() {

    val history: StateFlow<List<HistoryEntry>> =
        ServiceLocator.history.history.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _permissions = MutableStateFlow(PermissionUiState())
    val permissions: StateFlow<PermissionUiState> = _permissions.asStateFlow()

    fun refreshPermissions(context: Context) {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _permissions.value = PermissionUiState(
            accessibility = PermissionManager.isAccessibilityEnabled(context),
            overlay = PermissionManager.canDrawOverlays(context),
            notifications = notificationsGranted,
        )
    }

    fun clearHistory() = ServiceLocator.history.clear()
}

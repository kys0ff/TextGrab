package off.kys.textgrab.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PermissionUiState(
    val accessibility: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
)

data class MainState(
    val history: List<HistoryEntry> = emptyList(),
    val permissions: PermissionUiState = PermissionUiState()
)

sealed interface MainEvent {
    data object RefreshPermissions : MainEvent
    data object ClearHistory : MainEvent
}

class MainViewModel(
    private val context: Context,
    private val historyRepository: HistoryRepository,
    private val permissionManager: PermissionManager
) : ScreenModel {

    private val _permissions = MutableStateFlow(PermissionUiState())
    
    val state: StateFlow<MainState> = combine(
        historyRepository.history,
        _permissions
    ) { history, permissions ->
        MainState(history, permissions)
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainState()
    )

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.RefreshPermissions -> refreshPermissions()
            MainEvent.ClearHistory -> historyRepository.clear()
        }
    }

    private fun refreshPermissions() {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _permissions.value = PermissionUiState(
            accessibility = permissionManager.isAccessibilityEnabled(),
            overlay = permissionManager.canDrawOverlays(),
            notifications = notificationsGranted,
        )
    }
}

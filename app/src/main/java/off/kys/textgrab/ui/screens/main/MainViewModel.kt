package off.kys.textgrab.ui.screens.main

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.data.HistoryRepository
import off.kys.textgrab.utils.copy

class MainViewModel(
    private val application: Application,
    private val historyRepository: HistoryRepository,
    private val permissionManager: PermissionManager
) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(PermissionUiState())

    val state: StateFlow<MainState> = combine(
        historyRepository.history,
        _permissions
    ) { history, permissions ->
        MainState(history, permissions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainState()
    )

    fun onEvent(event: MainEvent) = when (event) {
        MainEvent.RefreshPermissions -> refreshPermissions()
        MainEvent.ClearHistory -> historyRepository.clear()
        is MainEvent.OnHistoryCopy -> application.copy(event.entry.text)
    }

    private fun refreshPermissions() {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                application, Manifest.permission.POST_NOTIFICATIONS
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

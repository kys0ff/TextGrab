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
import off.kys.textgrab.data.SettingsRepository
import off.kys.textgrab.utils.copy

class MainViewModel(
    private val application: Application,
    private val historyRepository: HistoryRepository,
    private val permissionManager: PermissionManager,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(PermissionUiState())
    private val _showClearHistoryConfirmation = MutableStateFlow(false)
    private val _showDonationDialog = MutableStateFlow(false)

    val state: StateFlow<MainState> = combine(
        historyRepository.history,
        _permissions,
        _showClearHistoryConfirmation,
        settingsRepository.showDonationIcon,
        _showDonationDialog
    ) { history, permissions, showClearHistory, showDonationIcon, showDonationDialog ->
        MainState(
            history = history,
            permissions = permissions,
            showClearHistoryConfirmation = showClearHistory,
            showDonationIcon = showDonationIcon,
            showDonationDialog = showDonationDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainState()
    )

    fun onEvent(event: MainEvent) = when (event) {
        MainEvent.RefreshPermissions -> refreshPermissions()
        MainEvent.ClearHistory -> {
            _showClearHistoryConfirmation.value = true
        }

        MainEvent.ConfirmClearHistory -> {
            _showClearHistoryConfirmation.value = false
            historyRepository.clear()
        }

        MainEvent.DismissClearHistoryDialog -> {
            _showClearHistoryConfirmation.value = false
        }

        is MainEvent.OnHistoryCopy -> application.copy(event.entry.text)

        MainEvent.OpenDonationDialog -> {
            _showDonationDialog.value = true
        }

        MainEvent.DismissDonationDialog -> {
            _showDonationDialog.value = false
        }

        MainEvent.RemoveDonationIcon -> {
            _showDonationDialog.value = false
            settingsRepository.hideDonationIcon()
        }
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

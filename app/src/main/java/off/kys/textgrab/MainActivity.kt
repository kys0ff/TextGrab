package off.kys.textgrab

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.ui.main.MainScreen
import off.kys.textgrab.ui.main.MainViewModel
import off.kys.textgrab.ui.theme.TextGrabTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result not required — the OCR service degrades gracefully without it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            TextGrabTheme {
                val permissions by viewModel.permissions.collectAsStateWithLifecycle()
                val history by viewModel.history.collectAsStateWithLifecycle()

                MainScreen(
                    permissions = permissions,
                    history = history,
                    onOpenAccessibility = { PermissionManager.openAccessibilitySettings(this) },
                    onOpenOverlay = { PermissionManager.openOverlaySettings(this) },
                    onClearHistory = viewModel::clearHistory,
                    onCopyHistory = ::copyToClipboard,
                    onScanNow = {
                        // Step out of the way so the accessibility service reads the
                        // previously-foregrounded app rather than TextGrab itself.
                        moveTaskToBack(true)
                        OverlayBus.send(OverlayCommand.Trigger(OverlayBus.mode.value))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions can change while we were in Settings — refresh on every resume.
        viewModel.refreshPermissions(this)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TextGrab", text))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

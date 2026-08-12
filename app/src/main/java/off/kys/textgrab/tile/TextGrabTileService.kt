package off.kys.textgrab.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import off.kys.textgrab.MainActivity
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus
import org.koin.android.ext.android.inject

/**
 * System-bar Quick Settings tile.
 */
class TextGrabTileService : TileService() {

    private val permissionManager: PermissionManager by inject()

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (isReady()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        unlockAndRun {
            val target = if (isReady()) {
                Intent(this, ScanTrampolineActivity::class.java)
                    .putExtra(ScanTrampolineActivity.EXTRA_MODE, OverlayBus.mode.value.name)
            } else {
                Intent(this, MainActivity::class.java)
            }
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchCollapsing(target)
        }
    }

    private fun isReady(): Boolean =
        permissionManager.isAccessibilityEnabled() &&
            permissionManager.canDrawOverlays()

    private fun launchCollapsing(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @SuppressLint("StartActivityAndCollapseDeprecated")
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

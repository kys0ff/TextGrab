package off.kys.textgrab.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import off.kys.textgrab.MainActivity
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus

/**
 * System-bar Quick Settings tile. Tapping it:
 *  - if the Accessibility service **and** overlay permission are both granted,
 *    launches the invisible [ScanTrampolineActivity] (which collapses the shade and
 *    kicks off a scan of the underlying app);
 *  - otherwise opens [MainActivity] so the user can finish setup.
 */
class TextGrabTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (isReady()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        // unlockAndRun defers the action until the device is unlocked (needed because
        // we launch an activity / show an overlay).
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
        PermissionManager.isAccessibilityEnabled(applicationContext) &&
            PermissionManager.canDrawOverlays(applicationContext)

    private fun launchCollapsing(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: startActivityAndCollapse takes a PendingIntent.
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

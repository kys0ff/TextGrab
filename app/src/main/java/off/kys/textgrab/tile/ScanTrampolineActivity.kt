package off.kys.textgrab.tile

import android.app.Activity
import android.os.Bundle
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.overlay.OverlayBus

/**
 * A zero-UI trampoline launched by [TextGrabTileService] via
 * `startActivityAndCollapse`. Its only purpose is to collapse the Quick Settings
 * shade (a side effect of being launched that way) and emit a scan [OverlayCommand].
 *
 * By finishing instantly, the app the user was actually looking at returns to the
 * foreground; the accessibility service then applies a short settle delay before
 * reading `rootInActiveWindow`, so it reads the target window and not this activity
 * or the shade.
 */
class ScanTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent?.getStringExtra(EXTRA_MODE)
            ?.let { runCatching { ExtractionMode.valueOf(it) }.getOrNull() }
            ?: ExtractionMode.ACCESSIBILITY

        OverlayBus.send(OverlayCommand.Trigger(mode))

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}

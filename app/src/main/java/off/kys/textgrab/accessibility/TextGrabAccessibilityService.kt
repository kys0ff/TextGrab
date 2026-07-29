package off.kys.textgrab.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import off.kys.textgrab.R
import off.kys.textgrab.core.clipboard.ClipboardHelper
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.ocr.MediaProjectionRequestActivity
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Primary text engine **and** host for the floating overlay.
 *
 * The service is a long-lived, always-permitted context, which makes it the ideal
 * owner of the [OverlayController] (it can legitimately add a
 * `TYPE_APPLICATION_OVERLAY` window and directly read `rootInActiveWindow`). It
 * listens on [OverlayBus] for commands emitted by the Quick Settings tile, the
 * overlay UI itself, and the OCR service.
 */
class TextGrabAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: OverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        overlay = OverlayController(
            context = this,
            onCopy = { text, source ->
                ClipboardHelper.copy(this, text, source, getString(R.string.copied_toast))
            },
            onCopyAll = { items, source ->
                ClipboardHelper.copyAll(
                    this, items, source, getString(R.string.copied_multi_toast, items.size),
                )
            },
            onSwitchMode = { mode -> OverlayBus.send(OverlayCommand.SwitchMode(mode)) },
            onSwitchLanguage = { lang ->
                OverlayBus.ocrLanguage.value = lang
                OverlayBus.send(OverlayCommand.Rescan)
            },
            onRescan = { OverlayBus.send(OverlayCommand.Rescan) },
            onClose = { OverlayBus.send(OverlayCommand.Hide) },
        )

        scope.launch {
            OverlayBus.commands.collect { command -> handle(command) }
        }
    }

    // We don't react to individual events — scans are user-triggered — but the
    // callback must be implemented.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private fun handle(command: OverlayCommand) {
        when (command) {
            is OverlayCommand.Trigger -> {
                OverlayBus.mode.value = command.mode
                startScan(command.mode)
            }

            is OverlayCommand.SwitchMode -> {
                OverlayBus.mode.value = command.mode
                startScan(command.mode)
            }

            OverlayCommand.Rescan -> startScan(OverlayBus.mode.value)

            OverlayCommand.ShowResults -> showOverlay()

            OverlayCommand.Hide -> hideOverlay()
        }
    }

    private fun startScan(mode: ExtractionMode) = when (mode) {
        ExtractionMode.ACCESSIBILITY -> scanAccessibility()
        ExtractionMode.OCR -> requestOcr()
    }

    private fun scanAccessibility() {
        OverlayBus.status.value = OverlayStatus.Scanning
        scope.launch {
            // Give the QS shade time to collapse and the target app to return to the
            // foreground so `rootInActiveWindow` points at the right window.
            delay(SETTLE_DELAY_MS)
            val nodes = AccessibilityExtractor.extract(rootInActiveWindow)
            OverlayBus.elements.value = nodes
            OverlayBus.status.value =
                if (nodes.isEmpty()) OverlayStatus.Empty else OverlayStatus.Ready(nodes.size)
            showOverlay()
        }
    }

    private fun requestOcr() {
        // The overlay must be off-screen while MediaProjection snapshots the display,
        // otherwise we would OCR our own UI. Hide now; ScreenCaptureService emits
        // ShowResults once recognition completes.
        hideOverlay()
        OverlayBus.status.value = OverlayStatus.Scanning
        startActivity(
            Intent(this, MediaProjectionRequestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun showOverlay() {
        overlay?.show()
        OverlayBus.visible.value = true
    }

    private fun hideOverlay() {
        overlay?.hide()
        OverlayBus.visible.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        overlay?.destroy()
        overlay = null
        scope.cancel()
        if (instance === this) instance = null
    }

    companion object {
        @Volatile
        var instance: TextGrabAccessibilityService? = null
            private set

        private const val SETTLE_DELAY_MS = 350L
    }
}

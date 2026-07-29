@file:SuppressLint("AccessibilityPolicy")

package off.kys.textgrab.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import off.kys.textgrab.R
import off.kys.textgrab.ServiceLocator
import off.kys.textgrab.core.clipboard.ClipboardHelper
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.ocr.MediaProjectionRequestActivity
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.overlay.OverlayController
import kotlin.time.Duration.Companion.milliseconds

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

    /**
     * Ensures only one accessibility scan is active at a time.
     *
     * Without this, rapid taps (or repeated rescans) could allow older scans to
     * finish after newer ones and overwrite the latest results.
     */
    private var scanJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        overlay = OverlayController(
            context = this,
            onCopyAll = { items, source ->
                ClipboardHelper.copyAll(
                    this,
                    items,
                    source,
                    getString(R.string.copied_multi_toast, items.size),
                )
            },
            onSwitchMode = { mode ->
                OverlayBus.send(OverlayCommand.SwitchMode(mode))
            },
            onSwitchLanguage = { lang ->
                OverlayBus.ocrLanguage.value = lang
                OverlayBus.send(OverlayCommand.Rescan)
            },
            onRescan = {
                OverlayBus.send(OverlayCommand.Rescan)
            },
            onClose = {
                OverlayBus.send(OverlayCommand.Hide)
            },
        )

        scope.launch {
            OverlayBus.commands.collect(::handle)
        }

        scope.launch {
            OverlayBus.mode.collect { mode ->
                if (mode == ExtractionMode.OCR) {
                    ServiceLocator.ocr.prepare(this@TextGrabAccessibilityService, OverlayBus.ocrLanguage.value)
                }
            }
        }

        scope.launch {
            OverlayBus.ocrLanguage.collect { lang ->
                if (OverlayBus.mode.value == ExtractionMode.OCR) {
                    ServiceLocator.ocr.prepare(this@TextGrabAccessibilityService, lang)
                }
            }
        }
    }

    // Scans are explicitly user-triggered.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private fun handle(command: OverlayCommand) = when (command) {
        is OverlayCommand.Trigger -> {
            OverlayBus.mode.value = command.mode
            startScan(command.mode)
        }

        is OverlayCommand.SwitchMode -> {
            OverlayBus.mode.value = command.mode
            startScan(command.mode)
        }

        OverlayCommand.Rescan ->
            startScan(OverlayBus.mode.value)

        OverlayCommand.ShowResults ->
            showOverlay()

        OverlayCommand.Hide ->
            hideOverlay()
    }

    private fun startScan(mode: ExtractionMode) {
        // Prevent stale scans from completing after newer requests.
        scanJob?.cancel()

        when (mode) {
            ExtractionMode.ACCESSIBILITY -> scanAccessibility()
            ExtractionMode.OCR -> requestOcr()
        }
    }

    private fun scanAccessibility() {
        OverlayBus.status.value = OverlayStatus.Scanning

        scanJob = scope.launch {
            // Allow QS panel/SystemUI to disappear so the active window belongs
            // to the foreground application.
            delay(SETTLE_DELAY_MS)

            val root = rootInActiveWindow
            if (root == null) {
                OverlayBus.elements.value = emptyList()
                OverlayBus.status.value = OverlayStatus.Empty
                showOverlay()
                return@launch
            }

            val nodes = AccessibilityExtractor.extract(root)

            OverlayBus.elements.value = nodes
            OverlayBus.status.value =
                if (nodes.isEmpty()) {
                    OverlayStatus.Empty
                } else {
                    OverlayStatus.Ready(nodes.size)
                }

            showOverlay()
        }
    }

    private fun requestOcr() {
        // Prevent an in-flight accessibility scan from reopening the overlay.
        scanJob?.cancel()

        // Hide overlay before MediaProjection captures the screen.
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
        scanJob?.cancel()

        hideOverlay()
        overlay?.destroy()
        overlay = null

        scope.cancel()

        if (instance === this) {
            instance = null
        }

        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TextGrabAccessibilityService? = null
            private set

        private val SETTLE_DELAY_MS = 350L.milliseconds
    }
}
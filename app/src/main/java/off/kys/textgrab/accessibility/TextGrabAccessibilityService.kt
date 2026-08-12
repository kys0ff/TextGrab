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
import off.kys.textgrab.core.clipboard.ClipboardHelper
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.ocr.MediaProjectionRequestActivity
import off.kys.textgrab.ocr.OcrEngine
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.overlay.OverlayController
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Primary text engine and host for the floating overlay.
 */
class TextGrabAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ocr: OcrEngine by inject()
    private val clipboardHelper: ClipboardHelper by inject()

    private var overlay: OverlayController? = null
    private var scanJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        overlay = OverlayController(
            context = this,
            onCopyAll = { items, source ->
                clipboardHelper.copyAll(
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
            onOpenDownload = {
                startActivity(
                    Intent(this, off.kys.textgrab.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("open_ocr_packages", true)
                )
                OverlayBus.send(OverlayCommand.Hide)
            }
        )

        scope.launch {
            OverlayBus.commands.collect(::handle)
        }

        scope.launch {
            OverlayBus.mode.collect { mode ->
                if (mode == ExtractionMode.OCR) {
                    ocr.prepare(this@TextGrabAccessibilityService, OverlayBus.ocrLanguage.value)
                }
            }
        }

        scope.launch {
            OverlayBus.ocrLanguage.collect { lang ->
                if (OverlayBus.mode.value == ExtractionMode.OCR) {
                    ocr.prepare(this@TextGrabAccessibilityService, lang)
                }
            }
        }
    }

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

        is OverlayCommand.SetScrollMode -> {
            OverlayBus.isScrollMode.value = command.enabled
            overlay?.updateScrollMode(command.enabled)
            if (!command.enabled) {
                startScan(OverlayBus.mode.value)
            }
            Unit
        }

        OverlayCommand.ShowResults ->
            showOverlay()

        OverlayCommand.Hide ->
            hideOverlay()
    }

    private fun startScan(mode: ExtractionMode) {
        scanJob?.cancel()

        when (mode) {
            ExtractionMode.ACCESSIBILITY -> scanAccessibility()
            ExtractionMode.OCR -> requestOcr()
        }
    }

    private fun scanAccessibility() {
        OverlayBus.status.value = OverlayStatus.Scanning

        scanJob = scope.launch {
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
        scanJob?.cancel()
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

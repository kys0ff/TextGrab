package off.kys.textgrab.overlay

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus

/**
 * A tiny in-process event bus shared by the Quick Settings tile, the accessibility
 * service (overlay host + primary engine) and the OCR screen-capture service.
 *
 * Everything runs in the app's single default process, so a plain object with hot
 * flows is the simplest reliable coordination channel, no IPC, binders or
 * broadcasts required.
 */
object OverlayBus {

    /** Commands consumed by [off.kys.textgrab.accessibility.TextGrabAccessibilityService]. */
    val commands = MutableSharedFlow<OverlayCommand>(
        replay = 0,
        extraBufferCapacity = 16,
    )

    /** The currently displayed text elements (screen-pixel coordinates). */
    val elements = MutableStateFlow<List<GrabbedText>>(emptyList())

    /** The active engine. Persisted only for the lifetime of the process. */
    val mode = MutableStateFlow(ExtractionMode.ACCESSIBILITY)

    /** The active OCR language. */
    val ocrLanguage = MutableStateFlow(OcrLanguage.BOTH)

    /** Coarse status driving the overlay header. */
    val status = MutableStateFlow<OverlayStatus>(OverlayStatus.Idle)

    /** Whether the overlay window is currently attached. */
    val visible = MutableStateFlow(false)

    fun send(command: OverlayCommand) {
        commands.tryEmit(command)
    }
}

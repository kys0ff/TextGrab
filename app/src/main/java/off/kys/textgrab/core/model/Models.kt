package off.kys.textgrab.core.model

import androidx.compose.runtime.Immutable

/**
 * Which engine produced a piece of text.
 *
 * [ACCESSIBILITY] is the primary, on-device, zero-capture engine that walks the
 * active window's [android.view.accessibility.AccessibilityNodeInfo] tree.
 * [OCR] is the Tesseract fallback fed by a MediaProjection screenshot.
 */
enum class ExtractionMode { ACCESSIBILITY, OCR }

/**
 * Supported OCR scripts.
 */
enum class OcrLanguage { LATIN, ARABIC, BOTH }

/**
 * A single selectable text element positioned in **screen pixel** coordinates.
 *
 * Bounds come either from [android.view.accessibility.AccessibilityNodeInfo.getBoundsInScreen]
 * (accessibility engine) or from a Tesseract text-line bounding box (OCR engine).
 */
@Immutable
data class GrabbedText(
    val id: Long,
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val source: ExtractionMode,
    val isRtl: Boolean = false,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

/** A copied entry persisted to the history log. */
@Immutable
data class HistoryEntry(
    val id: Long,
    val text: String,
    val timestamp: Long,
    val source: ExtractionMode,
)

/** Commands sent to the overlay host (the accessibility service). */
sealed interface OverlayCommand {
    /** Fresh scan requested from the tile / app, in the given [mode]. */
    data class Trigger(val mode: ExtractionMode) : OverlayCommand

    /** User toggled between engines while the overlay is open. */
    data class SwitchMode(val mode: ExtractionMode) : OverlayCommand

    /** Re-run the current mode's scan. */
    data object Rescan : OverlayCommand

    /** Display the overlay window with whatever results are currently in the bus.
     *  Emitted by the OCR service once recognition finishes. */
    data object ShowResults : OverlayCommand

    /** Tear the overlay window down. */
    data object Hide : OverlayCommand
}

/** Coarse overlay state used to drive the header / empty / error UI. */
sealed interface OverlayStatus {
    data object Idle : OverlayStatus
    data object Scanning : OverlayStatus
    data class Ready(val count: Int) : OverlayStatus
    data object Empty : OverlayStatus
    data class Error(val message: String) : OverlayStatus
}

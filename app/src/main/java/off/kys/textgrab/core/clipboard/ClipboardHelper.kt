package off.kys.textgrab.core.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import off.kys.textgrab.ServiceLocator
import off.kys.textgrab.core.model.ExtractionMode

/**
 * Unified copy manager. Handles Unicode / Arabic / Latin identically (Android's
 * clipboard is UTF-16 throughout) and records every copy in the history log.
 *
 * On Android 13+ the platform shows its own "copied" confirmation, so we suppress
 * our Toast there to avoid a double confirmation.
 */
object ClipboardHelper {

    private const val LABEL = "TextGrab"

    /** Copy a single element and log it. */
    fun copy(
        context: Context,
        text: String,
        source: ExtractionMode,
        toastMessage: String? = null,
    ) {
        if (text.isEmpty()) return
        clipboard(context).setPrimaryClip(ClipData.newPlainText(LABEL, text))
        ServiceLocator.history.add(text, source)
        maybeToast(context, toastMessage)
    }

    /**
     * Copy several elements joined with newlines (batch select). The joined blob is
     * logged as one entry to keep the history readable.
     */
    fun copyAll(
        context: Context,
        items: List<String>,
        source: ExtractionMode,
        toastMessage: String? = null,
    ) {
        val filtered = items.filter { it.isNotEmpty() }
        if (filtered.isEmpty()) return
        val joined = filtered.joinToString(separator = "\n")
        clipboard(context).setPrimaryClip(ClipData.newPlainText(LABEL, joined))
        ServiceLocator.history.add(joined, source)
        maybeToast(context, toastMessage)
    }

    private fun clipboard(context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun maybeToast(context: Context, message: String?) {
        // Android 13 (TIRAMISU) and above surface a system copy confirmation UI.
        if (message == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}

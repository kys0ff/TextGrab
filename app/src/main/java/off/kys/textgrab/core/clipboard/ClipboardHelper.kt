package off.kys.textgrab.core.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.data.HistoryRepository

/**
 * Unified copy manager. Handles Unicode / Arabic / Latin identically (Android's
 * clipboard is UTF-16 throughout) and records every copy in the history log.
 */
class ClipboardHelper(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

    /** Copy a single element and log it. */
    fun copy(
        text: String,
        source: ExtractionMode,
        toastMessage: String? = null,
    ) {
        if (text.isEmpty()) return
        clipboard().setPrimaryClip(ClipData.newPlainText(LABEL, text))
        historyRepository.add(text, source)
        maybeToast(toastMessage)
    }

    /**
     * Copy several elements joined with newlines (batch select). The joined blob is
     * logged as one entry to keep the history readable.
     */
    fun copyAll(
        items: List<String>,
        source: ExtractionMode,
        toastMessage: String? = null,
    ) {
        val filtered = items.filter { it.isNotEmpty() }
        if (filtered.isEmpty()) return
        val joined = filtered.joinToString(separator = "\n")
        clipboard().setPrimaryClip(ClipData.newPlainText(LABEL, joined))
        historyRepository.add(joined, source)
        maybeToast(toastMessage)
    }

    private fun clipboard(): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun maybeToast(message: String?) {
        if (message == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LABEL = "TextGrab"
    }
}

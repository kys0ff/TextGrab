package off.kys.textgrab.core.clipboard

import android.content.Context
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.data.HistoryRepository
import off.kys.textgrab.utils.copy

/**
 * Unified copy manager. Handles Unicode / Arabic / Latin identically (Android's
 * clipboard is UTF-16 throughout) and records every copy in the history log.
 */
class ClipboardHelper(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

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
        context.copy(joined, toastMessage)
        historyRepository.add(joined, source)
    }
}
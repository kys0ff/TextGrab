package off.kys.textgrab.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.isRtl

/**
 * Walks an [AccessibilityNodeInfo] tree (typically `rootInActiveWindow`) and
 * extracts visible text together with its screen bounds.
 *
 * The resulting list is sorted geometrically (top-to-bottom, then left-to-right),
 * which produces stable reading order across most applications.
 */
object AccessibilityExtractor {

    private data class DuplicateKey(
        val text: String,
        val left: Int,
        val top: Int,
    )

    fun extract(root: AccessibilityNodeInfo?): List<GrabbedText> {
        root ?: return emptyList()

        val results = ArrayList<GrabbedText>(64)
        val seen = HashSet<DuplicateKey>(64)
        val bounds = Rect()

        var nextId = 0L

        fun visit(node: AccessibilityNodeInfo?) {
            node ?: return

            // Entire subtree is invisible.
            if (!node.isVisibleToUser) return

            node.getBoundsInScreen(bounds)

            // Ignore invalid bounds.
            if (bounds.width() <= 0 || bounds.height() <= 0) return

            val raw =
                node.text?.toString()?.takeIf(String::isNotBlank)
                    ?: node.contentDescription?.toString()?.takeIf(String::isNotBlank)

            if (raw != null) {
                val text = raw.trim()

                val key = DuplicateKey(
                    text = text,
                    left = bounds.left,
                    top = bounds.top,
                )

                if (seen.add(key)) {
                    results += GrabbedText(
                        id = nextId++,
                        text = text,
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom,
                        source = ExtractionMode.ACCESSIBILITY,
                        isRtl = text.isRtl(),
                    )
                }
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                visit(node.getChild(i))
            }
        }

        visit(root)

        results.sortWith(compareBy({ it.top }, { it.left }))

        return results
    }
}
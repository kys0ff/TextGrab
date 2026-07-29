package off.kys.textgrab.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.isRtl

/**
 * Walks an [AccessibilityNodeInfo] tree (the active window's `rootInActiveWindow`)
 * and flattens every visible, non-blank text node into a list of [GrabbedText],
 * carrying each node's on-screen bounds.
 *
 * Order is preserved as visual reading order (top-to-bottom, then start-to-end),
 * which reads correctly for both LTR and RTL layouts because the sort is purely
 * geometric.
 */
object AccessibilityExtractor {

    fun extract(root: AccessibilityNodeInfo?): List<GrabbedText> {
        root ?: return emptyList()

        val results = ArrayList<GrabbedText>()
        val bounds = Rect()
        var nextId = 0L

        fun visit(node: AccessibilityNodeInfo?) {
            node ?: return

            // Prefer visible text; fall back to the content description (icons, images
            // with a label, etc.). Blank / whitespace-only nodes are ignored.
            val raw = node.text?.toString()?.takeIf { it.isNotBlank() }
                ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }

            if (raw != null && node.isVisibleToUser) {
                node.getBoundsInScreen(bounds)
                if (bounds.width() > 0 && bounds.height() > 0) {
                    val text = raw.trim()
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

            for (i in 0 until node.childCount) {
                visit(node.getChild(i))
            }
        }

        visit(root)

        return results
            // Drop exact duplicates that overlap at the same position (common with
            // decorative / mirrored nodes).
            .distinctBy { "${it.text}@${it.left},${it.top}" }
            .sortedWith(compareBy({ it.top }, { it.left }))
    }
}

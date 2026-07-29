package off.kys.textgrab

import android.content.Context
import off.kys.textgrab.data.HistoryRepository

/**
 * Minimal manual dependency container. Everything TextGrab shares between its
 * activities, services and the tile lives here, initialised once from
 * [TextGrabApp.onCreate]. Keeps the app free of a DI framework for its size.
 */
object ServiceLocator {

    @Volatile
    private var historyRepo: HistoryRepository? = null

    val history: HistoryRepository
        get() = historyRepo ?: error("ServiceLocator.init() was not called")

    fun init(context: Context) {
        if (historyRepo == null) {
            synchronized(this) {
                if (historyRepo == null) {
                    historyRepo = HistoryRepository(context.applicationContext)
                }
            }
        }
    }
}

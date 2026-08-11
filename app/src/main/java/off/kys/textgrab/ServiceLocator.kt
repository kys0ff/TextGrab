package off.kys.textgrab

import android.content.Context
import off.kys.textgrab.data.HistoryRepository
import off.kys.textgrab.ocr.OcrEngine
import off.kys.textgrab.ocr.OcrPackageRepository

/**
 * Minimal manual dependency container. Everything TextGrab shares between its
 * activities, services and the tile lives here, initialized once from
 * [TextGrabApp.onCreate]. Keeps the app free of a DI framework for its size.
 */
object ServiceLocator {

    @Volatile
    private var historyRepo: HistoryRepository? = null

    @Volatile
    private var ocrEngine: OcrEngine? = null

    @Volatile
    private var ocrPackageRepo: OcrPackageRepository? = null

    val history: HistoryRepository
        get() = historyRepo ?: error("ServiceLocator.init() was not called")

    val ocr: OcrEngine
        get() = ocrEngine ?: synchronized(this) {
            ocrEngine ?: OcrEngine().also { ocrEngine = it }
        }

    val ocrPackages: OcrPackageRepository
        get() = ocrPackageRepo ?: error("ServiceLocator.init() was not called")

    fun init(context: Context) {
        if (historyRepo == null) {
            synchronized(this) {
                if (historyRepo == null) {
                    historyRepo = HistoryRepository(context.applicationContext)
                    ocrPackageRepo = OcrPackageRepository(context.applicationContext)
                }
            }
        }
    }
}

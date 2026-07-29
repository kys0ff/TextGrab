package off.kys.textgrab

import android.app.Application

class TextGrabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}

package off.kys.textgrab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import off.kys.textgrab.ocr.OcrPackageRepository
import off.kys.textgrab.ui.main.MainScreen
import off.kys.textgrab.ui.ocr.OcrPackageScreen
import off.kys.textgrab.ui.theme.TextGrabTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: OcrPackageRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startScreen = if (intent.getBooleanExtra("open_ocr_packages", false)) {
            OcrPackageScreen()
        } else {
            MainScreen()
        }

        setContent {
            TextGrabTheme {
                Navigator(startScreen) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            repository.close()
        }
    }
}

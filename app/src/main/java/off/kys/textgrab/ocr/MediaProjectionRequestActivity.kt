package off.kys.textgrab.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.overlay.OverlayBus

/**
 * Invisible activity whose sole job is to request MediaProjection consent (which
 * must originate from an Activity), then hand the grant token to
 * [ScreenCaptureService]. Finishes immediately with no transition.
 */
class MediaProjectionRequestActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            OverlayBus.status.value = OverlayStatus.Error(getString(R.string.overlay_denied))
            OverlayBus.send(OverlayCommand.ShowResults)
        }
        finishAndClose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        runCatching { launcher.launch(manager.createScreenCaptureIntent()) }
            .onFailure { finishAndClose() }
    }

    private fun finishAndClose() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

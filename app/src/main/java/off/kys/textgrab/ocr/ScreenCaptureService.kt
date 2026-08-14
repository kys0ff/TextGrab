package off.kys.textgrab.ocr

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.overlay.OverlayBus
import org.koin.android.ext.android.inject

/**
 * Foreground service that snapshots the display via MediaProjection.
 */
class ScreenCaptureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ocr: OcrEngine by inject()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.let {
            IntentCompat.getParcelableExtra(it, EXTRA_RESULT_DATA, Intent::class.java)
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            beginCapture(resultCode, data)
        } else {
            fail(getString(R.string.overlay_label_permission_denied))
        }
        return START_NOT_STICKY
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mp = manager.getMediaProjection(resultCode, data)
        if (mp == null) {
            fail(getString(R.string.overlay_label_permission_denied))
            return
        }
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = releaseCapture()
        }, mainHandler)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        virtualDisplay = mp.createVirtualDisplay(
            "TextGrabCapture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            mainHandler,
        )

        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            r.setOnImageAvailableListener(null, null)
            val bitmap = image.toBitmap(width, height)
            image.close()
            runOcr(bitmap)
        }, mainHandler)
    }

    private fun runOcr(bitmap: Bitmap) {
        scope.launch {
            val language = OverlayBus.ocrLanguage.value
            
            if (!ocr.isLoaded(language)) {
                OverlayBus.status.value = OverlayStatus.Error(getString(R.string.ocr_package_label_missing_generic))
                OverlayBus.send(OverlayCommand.ShowResults)
                bitmap.recycle()
                releaseCapture()
                stopEverything()
                return@launch
            }

            OverlayBus.status.value = OverlayStatus.Scanning
            OverlayBus.send(OverlayCommand.ShowResults)

            val results = runCatching { 
                ocr.recognize(bitmap, this@ScreenCaptureService, language) 
            }.getOrDefault(emptyList())
            bitmap.recycle()

            OverlayBus.elements.value = results
            OverlayBus.mode.value = ExtractionMode.OCR
            OverlayBus.status.value =
                if (results.isEmpty()) OverlayStatus.Empty else OverlayStatus.Ready(results.size)
            OverlayBus.send(OverlayCommand.ShowResults)

            releaseCapture()
            stopEverything()
        }
    }

    private fun fail(message: String) {
        OverlayBus.status.value = OverlayStatus.Error(message)
        OverlayBus.send(OverlayCommand.ShowResults)
        releaseCapture()
        stopEverything()
    }

    private fun startAsForeground() {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_capture_label_title))
            .setContentText(getString(R.string.notif_capture_label_text))
            .setSmallIcon(R.drawable.ic_tile_scan)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun ensureChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_capture_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun releaseCapture() {
        mainHandler.post {
            runCatching { virtualDisplay?.release() }
            runCatching { imageReader?.close() }
            runCatching { projection?.stop() }
            virtualDisplay = null
            imageReader = null
            projection = null
        }
    }

    private fun stopEverything() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCapture()
        scope.cancel()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val CHANNEL_ID = "textgrab_capture"
        private const val NOTIF_ID = 4711
    }
}

private fun Image.toBitmap(width: Int, height: Int): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * width

    val padded = createBitmap(width + rowPadding / pixelStride, height)
    padded.copyPixelsFromBuffer(buffer)

    if (rowPadding == 0) return padded
    val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
    if (cropped !== padded) padded.recycle()
    return cropped
}

package off.kys.textgrab.ocr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import off.kys.textgrab.MainActivity
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.TesseractVersion
import org.koin.android.ext.android.inject

class OcrDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var downloadJob: Job? = null
    private val repository: OcrPackageRepository by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tessCode = intent?.getStringExtra(EXTRA_TESS_CODE) ?: return START_NOT_STICKY
        val versionStr = intent.getStringExtra(EXTRA_VERSION) ?: return START_NOT_STICKY
        val version = try { TesseractVersion.valueOf(versionStr) } catch (_: Exception) { return START_NOT_STICKY }
        val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: tessCode

        val notification = createNotification(displayName, "0%", 0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        downloadJob = serviceScope.launch {
            val success = TessDataStore.download(
                this@OcrDownloadService,
                tessCode,
                version,
                url,
                onProgress = { progress ->
                    val progressInt = (progress * 100).toInt()
                    updateNotification(displayName, "$progressInt%", progressInt)
                    repository.updateDownloadState(
                        "${tessCode}_$version",
                        DownloadState.Downloading(progress)
                    )
                }
            )

            if (success) {
                repository.updateDownloadState("${tessCode}_$version", DownloadState.Downloaded)
                repository.refreshInstallationStates()
                showCompletionNotification(displayName, true)
            } else {
                repository.updateDownloadState("${tessCode}_$version", DownloadState.Error("Download failed"))
                showCompletionNotification(displayName, false)
            }
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(title: String, progressText: String, progress: Int): android.app.Notification {
        val channelId = "ocr_downloads"
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notif_download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_download_label_title))
            .setContentText(getString(R.string.notif_download_label_desc, title, progressText))
            .setSmallIcon(R.drawable.ic_tile_scan)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, progressText: String, progress: Int) {
        val notification = createNotification(title, progressText, progress)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, success: Boolean) {
        val channelId = "ocr_downloads"
        val message = if (success) getString(R.string.notif_download_label_success, title) 
                      else getString(R.string.notif_download_label_failed, title)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.common_app_label_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_tile_scan)
            .setAutoCancel(true)
            .build()
        
        getSystemService(NotificationManager::class.java).notify(title.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_TESS_CODE = "tess_code"
        const val EXTRA_VERSION = "version"
        const val EXTRA_URL = "url"
        const val EXTRA_DISPLAY_NAME = "display_name"

        fun start(context: Context, tessCode: String, version: TesseractVersion, url: String, displayName: String) {
            val intent = Intent(context, OcrDownloadService::class.java).apply {
                putExtra(EXTRA_TESS_CODE, tessCode)
                putExtra(EXTRA_VERSION, version.name)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
            }
            context.startForegroundService(intent)
        }
    }
}

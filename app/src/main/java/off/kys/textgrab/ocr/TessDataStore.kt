package off.kys.textgrab.ocr

import android.content.Context
import off.kys.textgrab.ocr.model.TesseractVersion
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Manages Tesseract language data files.
 * Tesseract requires language files to be available on the filesystem (not inside assets).
 */
object TessDataStore {

    private const val TESS_DATA_ROOT = "ocr_data"

    /**
     * Returns the absolute path to the directory containing 'tessdata/' for the
     * specific engine version.
     */
    fun getTessDataPath(context: Context, version: TesseractVersion): String {
        val versionDir = File(context.filesDir, "$TESS_DATA_ROOT/${version.name.lowercase()}")
        val tessDir = File(versionDir, "tessdata")
        if (!tessDir.exists()) {
            tessDir.mkdirs()
        }
        return versionDir.absolutePath
    }

    fun isInstalled(context: Context, tessCode: String, version: TesseractVersion): Boolean {
        val file = File(getTessDataPath(context, version), "tessdata/$tessCode.traineddata")
        return file.exists() && file.length() > 0
    }

    fun getInstalledSize(context: Context, tessCode: String, version: TesseractVersion): Long {
        val file = File(getTessDataPath(context, version), "tessdata/$tessCode.traineddata")
        return if (file.exists()) file.length() else 0L
    }

    fun hasAnyInstalled(context: Context, tessCode: String): Boolean {
        return TesseractVersion.entries.any { isInstalled(context, it, tessCode) }
    }

    fun isInstalled(context: Context, version: TesseractVersion, tessCode: String): Boolean {
        val file = File(getTessDataPath(context, version), "tessdata/$tessCode.traineddata")
        return file.exists() && file.length() > 0
    }

    suspend fun download(
        context: Context,
        tessCode: String,
        version: TesseractVersion,
        url: String,
        onProgress: (Float) -> Unit
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dest = File(getTessDataPath(context, version), "tessdata/$tessCode.traineddata")
        val tempFile = File(dest.absolutePath + ".tmp")
        
        try {
            URL(url).openConnection().apply {
                connectTimeout = 10000
                readTimeout = 10000
            }.getInputStream().use { input ->
                val total = try { URL(url).openConnection().contentLengthLong } catch (_: Exception) { -1L }
                var downloaded = 0L
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                        bytes = input.read(buffer)
                    }
                }
            }
            if (tempFile.renameTo(dest)) {
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
        false
    }

    fun delete(context: Context, tessCode: String, version: TesseractVersion): Boolean {
        val file = File(getTessDataPath(context, version), "tessdata/$tessCode.traineddata")
        return if (file.exists()) file.delete() else false
    }
}

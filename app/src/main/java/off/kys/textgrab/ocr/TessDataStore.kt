package off.kys.textgrab.ocr

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Manages Tesseract language data files.
 * Tesseract requires language files to be available on the filesystem (not inside assets).
 */
object TessDataStore {

    private const val TESS_DATA_DIR = "tessdata"

    /**
     * Ensures that the traineddata for every language in [tessLanguage] (a
     * Tesseract language string such as "eng", "ara" or "ara+eng") is available
     * in the internal storage.
     * Returns the absolute path to the directory containing 'tessdata/'.
     */
    fun getTessDataPath(context: Context, tessLanguage: String): String {
        val dir = File(context.filesDir, TESS_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        for (language in tessLanguage.split('+')) {
            val file = File(dir, "$language.traineddata")
            if (!file.exists()) {
                copyFromAssets(context, file)
            }
        }

        // Tesseract init() expects the path to the directory *containing* 'tessdata'
        return context.filesDir.absolutePath
    }

    private fun copyFromAssets(context: Context, destination: File) {
        context.assets.open("$TESS_DATA_DIR/${destination.name}").use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
}

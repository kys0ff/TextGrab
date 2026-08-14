package off.kys.textgrab.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import off.kys.textgrab.R

val Context.clipboardManager: ClipboardManager get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.copy(
    text: String,
    copyMessage: String? = getString(R.string.common_toast_copied),
    label: String = packageManager.getApplicationLabel(applicationInfo).toString()
) {
    fun maybeToast(message: String?) {
        if (message == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        toast(message)
    }
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    maybeToast(copyMessage)
}
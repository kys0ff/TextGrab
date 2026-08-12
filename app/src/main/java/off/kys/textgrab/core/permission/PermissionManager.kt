package off.kys.textgrab.core.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import off.kys.textgrab.accessibility.TextGrabAccessibilityService

/**
 * Central place for checking and requesting the three permissions TextGrab needs.
 */
class PermissionManager(private val context: Context) {

    /** SYSTEM_ALERT_WINDOW — required for the TYPE_APPLICATION_OVERLAY window. */
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    /** Whether our [TextGrabAccessibilityService] is currently enabled by the user. */
    fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context, TextGrabAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        for (component in splitter) {
            val parsed = ComponentName.unflattenFromString(component) ?: continue
            if (parsed == expected) return true
        }
        return false
    }

    fun openAccessibilitySettings() {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openOverlaySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

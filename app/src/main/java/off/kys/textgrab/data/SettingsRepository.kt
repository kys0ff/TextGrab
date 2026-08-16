package off.kys.textgrab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _showDonationIcon = MutableStateFlow(prefs.getBoolean(PREF_SHOW_DONATION_ICON, true))
    val showDonationIcon: StateFlow<Boolean> = _showDonationIcon.asStateFlow()

    fun hideDonationIcon() {
        prefs.edit { putBoolean(PREF_SHOW_DONATION_ICON, false) }
        _showDonationIcon.value = false
    }

    companion object {
        private const val PREF_SHOW_DONATION_ICON = "show_donation_icon"
    }
}

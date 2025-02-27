package kodama.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import kodama.preferences.PreferenceStore
import kodama.preferences.getEnum

class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun theme() = preferenceStore.getEnum("pref_theme", Theme.SYSTEM)

    enum class Theme(val localizedString: String) {
        DARK("Dark"),
        LIGHT("Light"),
        SYSTEM("System");

        @Composable
        fun isDark(): Boolean {
            if (this == SYSTEM) return isSystemInDarkTheme()
            return this == DARK
        }
    }
}

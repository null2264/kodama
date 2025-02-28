package kodama.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import kodama.preferences.PreferenceStore
import kodama.preferences.getEnum
import kodama.resources.*
import org.jetbrains.compose.resources.StringResource

class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun theme() = preferenceStore.getEnum("pref_theme", Theme.SYSTEM)

    enum class Theme(val localizedString: StringResource) {
        DARK(Res.string.pref_dark_theme),
        LIGHT(Res.string.pref_light_theme),
        SYSTEM(Res.string.pref_system_theme);

        @Composable
        fun isDark(): Boolean {
            if (this == SYSTEM) return isSystemInDarkTheme()
            return this == DARK
        }
    }
}

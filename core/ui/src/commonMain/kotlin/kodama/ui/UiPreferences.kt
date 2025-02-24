package kodama.ui

import kodama.preferences.PreferenceStore

class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun nightMode() = preferenceStore.getBoolean("pref_night_mode", false)
}

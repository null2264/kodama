package kodama.ui

import kodama.preference.PreferenceStore

class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun nightMode() = preferenceStore.getBoolean("pref_night_mode", false)
}

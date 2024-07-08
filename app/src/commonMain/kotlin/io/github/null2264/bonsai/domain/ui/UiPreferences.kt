package io.github.null2264.bonsai.domain.ui

import bonsai.core.preference.PreferenceStore

class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun nightMode() = preferenceStore.getBoolean("pref_night_mode", false)
}

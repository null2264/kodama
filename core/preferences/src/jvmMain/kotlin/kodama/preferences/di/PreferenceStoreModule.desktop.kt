package kodama.preferences.di

import com.russhwolf.settings.PreferencesSettings
import kodama.preferences.PreferenceStore
import kodama.preferences.internal.SettingsPreferenceStore
import org.koin.core.scope.Scope

actual fun Scope.setupPreferenceStoreFactory(): PreferenceStore.Factory =
    SettingsPreferenceStore.Factory(PreferencesSettings.Factory())

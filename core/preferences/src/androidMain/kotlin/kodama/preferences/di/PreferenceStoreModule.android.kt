package kodama.preferences.di

import com.russhwolf.settings.SharedPreferencesSettings
import kodama.preferences.PreferenceStore
import kodama.preferences.internal.SettingsPreferenceStore
import org.koin.core.scope.Scope

actual fun Scope.setupPreferenceStoreFactory(): PreferenceStore.Factory =
    SettingsPreferenceStore.Factory(SharedPreferencesSettings.Factory(get()))

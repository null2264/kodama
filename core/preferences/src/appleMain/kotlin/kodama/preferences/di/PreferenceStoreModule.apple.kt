package kodama.preferences.di

import com.russhwolf.settings.NSUserDefaultsSettings
import kodama.preferences.PreferenceStore
import kodama.preferences.internal.SettingsPreferenceStore
import org.koin.core.scope.Scope

actual fun Scope.setupPreferenceStoreFactory(): PreferenceStore.Factory =
    SettingsPreferenceStore.Factory(NSUserDefaultsSettings.Factory())

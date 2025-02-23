package kodama.preference.di

import kodama.preference.DarwinPreferenceStore
import kodama.preference.PreferenceStore
import org.koin.core.scope.Scope

actual fun Scope.setupPreferenceStore(): PreferenceStore = DarwinPreferenceStore()

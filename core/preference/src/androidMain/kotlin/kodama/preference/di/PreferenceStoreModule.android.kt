package kodama.preference.di

import kodama.preference.AndroidPreferenceStore
import kodama.preference.PreferenceStore
import org.koin.core.scope.Scope

actual fun Scope.setupPreferenceStore(): PreferenceStore = AndroidPreferenceStore(get())

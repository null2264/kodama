package kodama.preference.di

import kodama.preference.PreferenceStore
import org.koin.core.scope.Scope
import org.koin.dsl.module

val preferenceStoreModule = module {
    single<PreferenceStore> { setupPreferenceStore() }
}

expect fun Scope.setupPreferenceStore(): PreferenceStore

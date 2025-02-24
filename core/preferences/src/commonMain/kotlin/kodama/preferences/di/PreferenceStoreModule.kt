package kodama.preferences.di

import kodama.preferences.PreferenceStore
import org.koin.core.scope.Scope
import org.koin.dsl.module

val preferenceStoreModule = module {
    single<PreferenceStore.Factory> { setupPreferenceStoreFactory() }
    single<PreferenceStore> { get<PreferenceStore.Factory>().default() }
}

expect fun Scope.setupPreferenceStoreFactory(): PreferenceStore.Factory

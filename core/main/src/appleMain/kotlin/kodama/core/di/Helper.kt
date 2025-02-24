package kodama.core.di

import kodama.preferences.di.preferenceStoreModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(preferenceStoreModule)
    }
}

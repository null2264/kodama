package kodama.core.di

import kodama.preference.di.preferenceStoreModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(preferenceStoreModule)
    }
}

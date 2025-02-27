package kodama.core.di

import kodama.preferences.di.preferenceStoreModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

fun initKoin(
    appDeclaration: KoinApplication.() -> Unit = {},
    additionalDeclaration: KoinApplication.() -> Unit = {},
) {
    startKoin {
        appDeclaration()
        modules(supabaseModule, preferenceStoreModule)
        additionalDeclaration()
    }
}

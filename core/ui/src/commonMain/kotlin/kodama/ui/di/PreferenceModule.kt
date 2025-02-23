package kodama.ui.di

import kodama.ui.UiPreferences
import org.koin.dsl.module

val preferenceModule = module {
    single { UiPreferences(get()) }
}

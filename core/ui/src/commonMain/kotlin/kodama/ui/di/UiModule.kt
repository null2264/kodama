package kodama.ui.di

import kodama.ui.UiPreferences
import kodama.ui.presentation.auth.LoginScreenModel
import kodama.ui.presentation.utils.screenModel
import org.koin.dsl.module

val uiModule = module {
    screenModel { LoginScreenModel(get()) }
    single { UiPreferences(get()) }
}

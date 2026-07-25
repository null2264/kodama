package kodama.ui.di

import kodama.ui.UiPreferences
import kodama.ui.presentation.auth.AuthScreenModel
import kodama.ui.presentation.image.ImageUploaderScreenModel
import kodama.ui.presentation.utils.screenModel
import org.koin.dsl.module

val uiModule = module {
    screenModel { AuthScreenModel(get()) }
    screenModel { ImageUploaderScreenModel(get()) }
    single { UiPreferences(get()) }
}

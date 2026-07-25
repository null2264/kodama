package kodama.ui.di

import kodama.ui.UiPreferences
import kodama.ui.presentation.auth.AuthScreenModel
import kodama.ui.presentation.auth.OtpVerificationScreenModel
import kodama.ui.presentation.auth.TotpVerificationScreenModel
import kodama.ui.presentation.image.ImageUploaderScreenModel
import kodama.ui.presentation.settings.TotpSetupScreenModel
import kodama.ui.presentation.utils.screenModel
import org.koin.dsl.module

val uiModule = module {
    screenModel { AuthScreenModel(get()) }
    screenModel { params -> OtpVerificationScreenModel(get(), params.get()) }
    screenModel { params -> TotpVerificationScreenModel(get(), params.get(), params.get()) }
    screenModel { TotpSetupScreenModel(get()) }
    screenModel { ImageUploaderScreenModel(get()) }
    single { UiPreferences(get()) }
}

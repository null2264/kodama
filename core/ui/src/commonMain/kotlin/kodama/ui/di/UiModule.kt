package kodama.ui.di

import kodama.ui.UiPreferences
import kodama.ui.presentation.auth.AuthScreenModel
import kodama.ui.presentation.auth.OtpVerificationScreenModel
import kodama.ui.presentation.auth.TotpVerificationScreenModel
import kodama.ui.presentation.contest.ContestDetailScreenModel
import kodama.ui.presentation.contest.CreateBonsaiScreenModel
import kodama.ui.presentation.contest.CreateContestScreenModel
import kodama.ui.presentation.contest.EditContestScreenModel
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
    screenModel { CreateContestScreenModel(get(), get()) }
    screenModel { params -> ContestDetailScreenModel(get(), params.get()) }
    screenModel { params -> EditContestScreenModel(get(), get(), params.get()) }
    screenModel { params -> CreateBonsaiScreenModel(get(), get(), params.get(), params.get()) }
    single { UiPreferences(get()) }
}

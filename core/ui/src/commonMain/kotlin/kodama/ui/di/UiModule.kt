package kodama.ui.di

import coil3.ImageLoader
import kodama.ui.UiPreferences
import kodama.ui.presentation.auth.AuthScreenModel
import kodama.ui.presentation.auth.OtpVerificationScreenModel
import kodama.ui.presentation.auth.TotpVerificationScreenModel
import kodama.ui.presentation.contest.slop.AssignJudgesScreenModel
import kodama.ui.presentation.contest.slop.ContestDetailScreenModel
import kodama.ui.presentation.contest.slop.CreateBonsaiScreenModel
import kodama.ui.presentation.contest.slop.CreateContestScreenModel
import kodama.ui.presentation.contest.slop.EditContestScreenModel
import kodama.ui.presentation.contest.slop.FinalizeEntryScreenModel
import kodama.ui.presentation.bonsai.BonsaiDetailViewModel
import kodama.ui.presentation.contest.ContestViewModel
import kodama.ui.presentation.contest.slop.RatingScreenModel
import kodama.ui.presentation.contest.slop.ResultsScreenModel
import kodama.ui.presentation.home.HomeTabScreenModel
import kodama.ui.presentation.image.ImageUploaderScreenModel
import kodama.ui.presentation.main.MainScreenModel
import kodama.ui.presentation.profile.EditProfileScreenModel
import kodama.ui.presentation.settings.TotpSetupScreenModel
import kodama.ui.presentation.utils.screenModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val uiModule = module {
    single { ImageLoader(get()) }

    screenModel { HomeTabScreenModel(get()) }
    screenModel { AuthScreenModel(get()) }
    viewModel<ContestViewModel>()
    screenModel { MainScreenModel() }

    single { UiPreferences(get()) }

    // Mostly slop
    screenModel { params -> OtpVerificationScreenModel(get(), params.get()) }
    screenModel { params -> TotpVerificationScreenModel(get(), params.get(), params.get()) }
    screenModel { TotpSetupScreenModel(get()) }
    screenModel { ImageUploaderScreenModel(get()) }
    screenModel { CreateContestScreenModel(get(), get()) }
    screenModel { params -> ContestDetailScreenModel(get(), params.get()) }
    screenModel { params -> EditContestScreenModel(get(), get(), params.get()) }
    screenModel { params -> CreateBonsaiScreenModel(get(), get(), params.get(), params.get()) }
    screenModel { EditProfileScreenModel(get()) }
    screenModel { params -> AssignJudgesScreenModel(get(), params.get()) }
    viewModel<BonsaiDetailViewModel>()
    screenModel { params -> RatingScreenModel(get(), params.get(), params.get()) }
    screenModel { params -> ResultsScreenModel(get(), params.get()) }
    screenModel { params -> FinalizeEntryScreenModel(get(), get(), params.get(), params.get()) }
}

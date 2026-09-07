package kodama.ui.presentation.contest

import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
import kodama.core.util.isAdmin
import kodama.core.util.isJudge
import kodama.ui.presentation.utils.StateViewModel
import kodama.ui.presentation.utils.inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContestViewModel(
    private val contestId: String,
    private val contestRepository: ContestRepository,
    private val auth: Auth,
) : StateViewModel<ContestViewModel.State>(State()) {

    init {
        loadContest()
    }

    fun loadContest() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val classIds = contestRepository.getContestClassIds(contestId)
                val allClasses = contestRepository.getBonsaiClasses()
                val selectedClasses = allClasses.filter { it.id in classIds }
                val users = contestRepository.getContestUsers(contestId)
//                loadSheet()
                mutableState.update {
                    it.copy(
                        contest = contest,
                        classes = selectedClasses,
                        isLoading = false,
                        contestUsers = users,
                    )
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

//    suspend fun loadSheet() {
//        val auth = inject<Auth>()
//        val currentUser = auth.currentUserOrNull()
//        val isAdmin = currentUser.isAdmin
//        mutableState.update { it.copy(isSheetLoading = true) }
//        try {
//            val bonsaiList =
//                if (!isAdmin) {
//                    contestRepository.getMyBonsaiForContest(contestId)
//                } else {
//                    contestRepository.getBonsaiWithMetadataForContest(contestId)
//                }
//            mutableState.update { it.copy(isSheetLoading = false, bonsaiList = bonsaiList) }
//        } catch (_: Exception) {
//            mutableState.update { it.copy(isSheetLoading = false) }
//        }
//    }

    fun subscribeBonsaiList() = contestRepository.subscribeBonsaiListForContest(contestId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribeReviews(): Flow<List<Review>> {
        val currentUser = auth.currentUserOrNull()
        if (currentUser == null || !currentUser.isJudge(state.value.contestUsers)) return flowOf(listOf())

        return state.map { it.contestUsers }
            .distinctUntilChanged()
            .flatMapLatest { contestUsers ->
                if (currentUser.isJudge(contestUsers)) {
                    contestRepository.subscribeMyReviews(currentUser.id)
                } else {
                    flowOf(emptyList())
                }
            }
    }

    data class State(
        val contest: Contest? = null,
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isSheetLoading: Boolean = false,
        val contestUsers: List<ContestUser> = emptyList(),

        val isUpdatingState: Boolean = false,
//        val bonsaiList: List<Bonsai> = emptyList(),
        val reviews: List<Review> = emptyList(),
    )
}

package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
import kodama.core.util.isAdmin
import kodama.ui.presentation.utils.inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContestScreenModel(
    private val contestRepository: ContestRepository,
    private val contestId: String,
) : StateScreenModel<ContestScreenModel.State>(State()) {

    init {
        loadContest()
    }

    fun loadContest() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val classIds = contestRepository.getContestClassIds(contestId)
                val allClasses = contestRepository.getBonsaiClasses()
                val selectedClasses = allClasses.filter { it.id in classIds }
                val users = contestRepository.getContestUsers(contestId)
                loadSheet()
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

    suspend fun loadSheet() {
        val auth = inject<Auth>()
        val currentUser = auth.currentUserOrNull()
        val isAdmin = currentUser.isAdmin
        mutableState.update { it.copy(isSheetLoading = true) }
        try {
            val bonsaiList =
                if (!isAdmin) {
                    contestRepository.getMyBonsaiForContest(contestId)
                } else {
                    contestRepository.getBonsaiWithMetadataForContest(contestId)
                }
            mutableState.update { it.copy(isSheetLoading = false, bonsaiList = bonsaiList) }
        } catch (_: Exception) {
            mutableState.update { it.copy(isSheetLoading = false) }
        }
    }

    fun subscribeBonsaiList() = contestRepository.subscribeBonsaiListForContest(contestId)

    data class State(
        val contest: Contest? = null,
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isSheetLoading: Boolean = false,
        val contestUsers: List<ContestUser> = emptyList(),

        val isUpdatingState: Boolean = false,
        val bonsaiList: List<Bonsai> = emptyList(),
        val reviews: List<Review> = emptyList(),
    )
}

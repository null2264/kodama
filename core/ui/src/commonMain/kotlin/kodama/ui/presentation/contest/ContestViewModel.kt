package kodama.ui.presentation.contest

import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
import kodama.ui.presentation.utils.StateViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContestViewModel(
    private val contestId: String,
    private val contestRepository: ContestRepository,
    private val auth: Auth,
) : StateViewModel<ContestViewModel.State>(State()) {

    init {
        loadContest()
        subscribeRealtime()
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

    fun transitionContestState(
        newState: String,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            mutableState.update { it.copy(isUpdatingState = true) }
            try {
                contestRepository.updateContestState(contestId, newState)
                loadContest()
                mutableState.update { it.copy(isUpdatingState = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isUpdatingState = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    /**
     * Subscribe to realtime changes for bonsai list and reviews
     */
    fun subscribeRealtime() {
        viewModelScope.launch {
            mutableState.update { it.copy(isSheetLoading = true) }
            val currentUser = auth.currentUserOrNull()
            contestRepository.subscribeBonsaiListForContest(contestId).combine(
                currentUser?.let { contestRepository.subscribeMyReviews(it.id) } ?: flowOf()
            ) { bonsaiList, review ->
                Pair(bonsaiList, review)
            }.collect { (bonsaiList, reviews) ->
                mutableState.update {
                    it.copy(
                        bonsaiList = bonsaiList,
                        reviews = reviews,
                        isSheetLoading = false,
                    )
                }
            }
        }
    }

    fun deleteBonsai(
        bonsaiId: String,
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                contestRepository.deleteBonsai(bonsaiId)
                loadContest()
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun verifyBonsai(
        bonsaiId: String,
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            mutableState.update {
                val verifying = it.bonsaiIsVerifying.toMutableMap().apply {
                    put(bonsaiId, true)
                }
                it.copy(bonsaiIsVerifying = verifying)
            }

            try {
                contestRepository.verifyBonsai(bonsaiId)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            } finally {
                mutableState.update {
                    val verifying = it.bonsaiIsVerifying.toMutableMap().apply {
                        remove(bonsaiId)
                    }
                    it.copy(bonsaiIsVerifying = verifying)
                }
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
        val bonsaiList: List<Bonsai> = emptyList(),
        val reviews: List<Review> = emptyList(),
        val bonsaiIsVerifying: Map<String, Boolean> = emptyMap()
    )
}

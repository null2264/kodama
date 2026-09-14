package kodama.ui.presentation.contest

import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestClass
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
import kodama.core.util.isJudge
import kodama.ui.presentation.utils.StateViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
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
        subscribeSheet()
        subscribeContestUsers()
        loadContest()
    }

    fun loadContest() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val contestClasses = contestRepository.getContestClasses(contestId)
                val contestClassesActualIds = contestClasses.map { it.class_id }
                val allClasses = contestRepository.getBonsaiClasses()
                val selectedClasses = allClasses.filter { it.id in contestClassesActualIds }
                val users = contestRepository.getContestUsers(contestId)
                mutableState.update {
                    it.copy(
                        contest = contest,
                        contestClasses = contestClasses,
                        classes = selectedClasses,
                        contestUsers = users,
                        isLoading = false,
                    )
                }

            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun subscribeContestUsers() = viewModelScope.launch {
        contestRepository.watchContestUsers(contestId).collect {
            try {
                val users = contestRepository.getContestUsers(contestId)
                mutableState.update { it.copy(contestUsers = users) }
            } catch (_: Exception) {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribeSheet() = viewModelScope.launch {
        mutableState.update { it.copy(isSheetLoading = true) }

        state.map { it.contestUsers }
            .distinctUntilChanged()
            .flatMapLatest {
                val contestUsers = it.orEmpty()
                val currentUser = auth.currentUserOrNull()
                val isJudge = currentUser?.isJudge(contestUsers) == true

                val reviewFlow = when {
                    currentUser == null -> flowOf(emptyList())
                    isJudge -> contestRepository.subscribeMyReviews(currentUser.id)
                    else -> contestRepository.subscribeAllReviews()
                }

                contestRepository.subscribeBonsaiListForContest(contestId)
                    .combine(reviewFlow) { bonsaiList, reviews ->
                        bonsaiList to reviews
                    }
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

    fun refreshUsers() {
        viewModelScope.launch {
            try {
                val users = contestRepository.getContestUsers(contestId)
                mutableState.update {
                    it.copy(
                        contestUsers = users,
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

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
        val contestClasses: List<ContestClass> = emptyList(),
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isSheetLoading: Boolean = false,
        val contestUsers: List<ContestUser>? = emptyList(),

        val isUpdatingState: Boolean = false,
        val bonsaiList: List<Bonsai> = emptyList(),
        val reviews: List<Review> = emptyList(),
        val bonsaiIsVerifying: Map<String, Boolean> = emptyMap(),
    ) {
        val canFinalizeContest: Boolean
            get() {
                val hasHeadJudge = contestUsers?.any { it.role == "head_judge" }
                return hasHeadJudge == true || contestClasses.all { contestClass ->
                    contestUsers?.any { it.role == "judge" && it.contest_class_id == contestClass.id } ?: false
                }
            }
    }
}

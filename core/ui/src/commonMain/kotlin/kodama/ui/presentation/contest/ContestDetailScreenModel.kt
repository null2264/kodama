package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.Bonsai
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContestDetailScreenModel(
    private val contestRepository: ContestRepository,
    private val contestId: String,
) : StateScreenModel<ContestDetailScreenModel.State>(State()) {

    init {
        loadContest()
    }

    private fun loadContest() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val classIds = contestRepository.getContestClassIds(contestId)
                val allClasses = contestRepository.getBonsaiClasses()
                val selectedClasses = allClasses.filter { it.id in classIds }
                mutableState.update {
                    it.copy(
                        contest = contest,
                        classes = selectedClasses,
                        isLoading = false,
                    )
                }
                loadSheetData(contest?.state)
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadSheetData(contestState: String?) {
        if (contestState != "accepting" && contestState != "reviewing") return
        mutableState.update { it.copy(isSheetLoading = true) }
        try {
            val bonsai = contestRepository.getBonsaiWithMetadataForContest(contestId)
            val myBonsai = contestRepository.getMyBonsaiForContest(contestId)
            val reviews = if (contestState == "reviewing") {
                contestRepository.getReviewsForContest(contestId)
            } else {
                emptyList()
            }
            val users = if (contestState == "reviewing") {
                contestRepository.getContestUsers(contestId)
            } else {
                emptyList()
            }
            mutableState.update {
                it.copy(
                    bonsaiList = bonsai,
                    myBonsai = myBonsai,
                    reviews = reviews,
                    contestUsers = users,
                    isSheetLoading = false,
                )
            }
        } catch (_: Exception) {
            mutableState.update { it.copy(isSheetLoading = false) }
        }
    }

    fun transitionContestState(
        newState: String,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        screenModelScope.launch {
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

    fun finishContest(
        force: Boolean = false,
        onError: (String) -> Unit = {},
        onSuccess: (String?) -> Unit,
    ) {
        screenModelScope.launch {
            mutableState.update { it.copy(isUpdatingState = true) }
            try {
                val winnerId = contestRepository.finishContest(contestId, force)
                loadContest()
                mutableState.update { it.copy(isUpdatingState = false) }
                onSuccess(winnerId)
            } catch (e: Exception) {
                mutableState.update { it.copy(isUpdatingState = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun verifyBonsai(
        bonsaiId: String,
        onError: (String) -> Unit = {},
    ) {
        screenModelScope.launch {
            try {
                contestRepository.verifyBonsai(bonsaiId)
                loadSheetData(state.value.contest?.state)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun finalizeBonsai(
        bonsaiId: String,
        onError: (String) -> Unit = {},
    ) {
        screenModelScope.launch {
            try {
                contestRepository.finalizeBonsai(bonsaiId)
                loadSheetData(state.value.contest?.state)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun deleteBonsai(
        bonsaiId: String,
        onError: (String) -> Unit = {},
    ) {
        screenModelScope.launch {
            try {
                contestRepository.deleteBonsai(bonsaiId)
                loadSheetData(state.value.contest?.state)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val contest: Contest? = null,
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isUpdatingState: Boolean = false,
        val bonsaiList: List<Bonsai> = emptyList(),
        val myBonsai: List<Bonsai> = emptyList(),
        val reviews: List<Review> = emptyList(),
        val contestUsers: List<ContestUser> = emptyList(),
        val isSheetLoading: Boolean = false,
    )
}

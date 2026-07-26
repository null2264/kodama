package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
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
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun finalizeContest(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        screenModelScope.launch {
            mutableState.update { it.copy(isFinalizing = true) }
            try {
                contestRepository.updateContestState(contestId, "accepting")
                loadContest()
                mutableState.update { it.copy(isFinalizing = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isFinalizing = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val contest: Contest? = null,
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isFinalizing: Boolean = false,
    )
}

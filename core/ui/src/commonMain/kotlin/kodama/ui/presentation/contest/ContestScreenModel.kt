package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kodama.core.data.Review
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

package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BonsaiDetailScreenModel(
    private val contestRepository: ContestRepository,
    private val auth: Auth,
    private val contestId: String,
    private val bonsaiId: String,
) : StateScreenModel<BonsaiDetailScreenModel.State>(State()) {

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val currentUserId = auth.currentUserOrNull()?.id
                val bonsai = contestRepository.getBonsaiById(bonsaiId)
                val contest = contestRepository.getContestById(contestId)
                val contestClasses = contestRepository.getBonsaiContestClasses(contestId)
                val bonsaiClass = contestClasses
                    .firstOrNull { it.id == bonsai?.contest_class_id }
                    ?.data

                val contestUsers = contestRepository.getContestUsers(contestId)
                val isJudge = currentUserId != null &&
                    contestUsers.any {
                        it.user_id == currentUserId &&
                            (it.role == "judge" || it.role == "head_judge")
                    }

                mutableState.update {
                    it.copy(
                        bonsai = bonsai,
                        contest = contest,
                        bonsaiClass = bonsaiClass,
                        isOwner = currentUserId != null && bonsai?.owner_id == currentUserId,
                        isJudge = isJudge,
                        qrUri = "kodama://$contestId/$bonsaiId",
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat data",
                    )
                }
            }
        }
    }

    data class State(
        val bonsai: Bonsai? = null,
        val contest: Contest? = null,
        val bonsaiClass: BonsaiClass? = null,
        val isOwner: Boolean = false,
        val isJudge: Boolean = false,
        val qrUri: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    )
}

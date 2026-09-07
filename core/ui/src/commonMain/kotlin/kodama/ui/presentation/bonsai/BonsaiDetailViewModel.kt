package kodama.ui.presentation.bonsai

import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.Review
import kodama.ui.presentation.utils.StateViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BonsaiDetailViewModel(
    private val contestId: String,
    private val bonsaiId: String,
    private val contestRepository: ContestRepository,
    private val auth: Auth,
) : StateViewModel<BonsaiDetailViewModel.State>(State()) {

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, isReviewLoading = true) }
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
                val reviews = getReviews()

                mutableState.update {
                    it.copy(
                        bonsai = bonsai,
                        contest = contest,
                        bonsaiClass = bonsaiClass,
                        isOwner = currentUserId != null && bonsai?.owner_id == currentUserId,
                        isJudge = isJudge,
                        qrUri = "kodama://$contestId/$bonsaiId",
                        isLoading = false,
                        reviews = reviews,
                        isReviewLoading = false,
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        isReviewLoading = false,
                        error = e.message ?: "Gagal memuat data",
                    )
                }
            }
        }
    }

    fun loadReview() {
        viewModelScope.launch {
            mutableState.update { it.copy(isReviewLoading = true) }
            try {
                val reviews = getReviews()
                mutableState.update {
                    it.copy(
                        reviews = reviews,
                        isReviewLoading = false,
                    )
                }
            } catch (e: Exception) {
                mutableState.update { it.copy(isReviewLoading = false) }
            }
        }
    }

    suspend fun getReviews() = contestRepository.getReviewsForBonsai(bonsaiId)

    data class State(
        val bonsai: Bonsai? = null,
        val contest: Contest? = null,
        val bonsaiClass: BonsaiClass? = null,
        val isOwner: Boolean = false,
        val isJudge: Boolean = false,
        val qrUri: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,

        val reviews: List<Review>? = null,
        val isReviewLoading: Boolean = false,
    )
}

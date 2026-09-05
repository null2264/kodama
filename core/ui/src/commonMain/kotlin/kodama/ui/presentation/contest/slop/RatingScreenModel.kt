package kodama.ui.presentation.contest.slop

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.Bonsai
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RatingScreenModel(
    private val contestRepository: ContestRepository,
    private val contestId: String,
    private val bonsaiId: String,
) : StateScreenModel<RatingScreenModel.State>(State()) {

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val allBonsai = contestRepository.getBonsaiWithMetadataForContest(contestId)
                val bonsai = allBonsai.firstOrNull { it.id == bonsaiId }
                mutableState.update {
                    it.copy(
                        contest = contest,
                        bonsai = bonsai,
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

    fun onScoreChanged(criteria: String, score: Int) {
        mutableState.update {
            it.copy(scores = it.scores.toMutableMap().apply { put(criteria, score.coerceIn(1, 100)) })
        }
    }

    fun onCommentsChanged(comments: String) {
        mutableState.update { it.copy(comments = comments) }
    }

    fun submitReview(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        val currentState = state.value
        val totalScore = currentState.scores.values.sum()

        screenModelScope.launch {
            mutableState.update { it.copy(isSubmitting = true) }
            try {
                contestRepository.submitReview(
                    bonsaiId = bonsaiId,
                    scores = currentState.scores,
                    totalScore = totalScore,
                    comments = currentState.comments.ifBlank { null },
                )
                mutableState.update { it.copy(isSubmitting = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isSubmitting = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val contest: Contest? = null,
        val bonsai: Bonsai? = null,
        val scores: Map<String, Int> = mapOf(
            "penampilan" to 50,
            "gerak_dasar" to 50,
            "keserasian" to 50,
            "kematangan" to 50,
        ),
        val comments: String = "",
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
    )
}

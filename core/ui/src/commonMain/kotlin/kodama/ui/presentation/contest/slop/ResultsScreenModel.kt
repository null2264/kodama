package kodama.ui.presentation.contest.slop

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.Bonsai
import kodama.core.data.BonsaiClass
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.Review
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResultsScreenModel(
    private val contestRepository: ContestRepository,
    private val contestId: String,
) : StateScreenModel<ResultsScreenModel.State>(State()) {

    init {
        loadResults()
    }

    private fun loadResults() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val bonsaiList = contestRepository.getBonsaiWithMetadataForContest(contestId)
                val reviews = contestRepository.getReviewsForContest(contestId)
                val contestClasses = contestRepository.getBonsaiContestClasses(contestId)
                val classMap = contestClasses.associate { it.class_id to it.data }

                val rankings = computeRankings(bonsaiList, reviews, classMap)
                val classWinners = computeClassWinners(rankings)
                val bestInShow = rankings.firstOrNull()

                mutableState.update {
                    it.copy(
                        contest = contest,
                        rankings = rankings,
                        classWinners = classWinners,
                        bestInShow = bestInShow,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat hasil",
                    )
                }
            }
        }
    }

    private fun computeRankings(
        bonsaiList: List<Bonsai>,
        reviews: List<Review>,
        classMap: Map<String, BonsaiClass>,
    ): List<RankingEntry> {
        val reviewsByBonsai = reviews.groupBy { it.bonsai_id }
        return bonsaiList.mapNotNull { bonsai ->
            val bonsaiReviews = reviewsByBonsai[bonsai.id]
            if (bonsaiReviews.isNullOrEmpty()) return@mapNotNull null

            val avgScore = bonsaiReviews.map { it.total_score }.average()
            val avgScores = mutableMapOf<String, Double>()
            val criteria = listOf("penampilan", "gerak_dasar", "keserasian", "kematangan")
            for (criterion in criteria) {
                val values = bonsaiReviews.mapNotNull { it.scores[criterion] }
                avgScores[criterion] = if (values.isNotEmpty()) values.average() else 0.0
            }

            RankingEntry(
                bonsai = bonsai,
                className = classMap[bonsai.contest_class_id]?.name ?: "",
                avgScore = avgScore,
                avgScores = avgScores,
                reviewCount = bonsaiReviews.size,
            )
        }.sortedByDescending { it.avgScore }
    }

    private fun computeClassWinners(rankings: List<RankingEntry>): List<RankingEntry> {
        return rankings
            .groupBy { it.bonsai.contest_class_id }
            .map { (_, entries) -> entries.first() }
            .sortedByDescending { it.avgScore }
    }

    data class RankingEntry(
        val bonsai: Bonsai,
        val className: String,
        val avgScore: Double,
        val avgScores: Map<String, Double>,
        val reviewCount: Int,
    )

    data class State(
        val contest: Contest? = null,
        val rankings: List<RankingEntry> = emptyList(),
        val classWinners: List<RankingEntry> = emptyList(),
        val bestInShow: RankingEntry? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
    )
}

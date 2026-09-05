package kodama.ui.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

class HomeTabScreenModel(
    private val contestRepository: ContestRepository,
) : StateScreenModel<HomeTabScreenModel.State>(State()) {

    init {
        loadContests()
    }

    fun loadContests() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            try {
                withTimeout(30_000L.milliseconds) {
                    val contests = contestRepository.getOpenContests()
                    mutableState.update { it.copy(contests = contests, isLoading = false) }
                }
            } catch (e: TimeoutCancellationException) {
                mutableState.update { it.copy(error = "Request timed out", isLoading = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(error = e.message ?: "Unknown error", isLoading = false) }
            }
        }
    }

    data class State(
        val contests: List<Contest> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )
}

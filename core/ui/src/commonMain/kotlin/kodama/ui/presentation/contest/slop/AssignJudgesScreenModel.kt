package kodama.ui.presentation.contest.slop

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.BonsaiContestClass
import kodama.core.data.ContestRepository
import kodama.core.data.ContestUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssignJudgesScreenModel(
    private val contestRepository: ContestRepository,
    private val contestId: String,
) : StateScreenModel<AssignJudgesScreenModel.State>(State()) {

    init {
        loadData()
    }

    private var searchJob: Job? = null

    private fun loadData() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contestClasses = contestRepository.getBonsaiContestClasses(contestId)
                val existingUsers = contestRepository.getContestUsers(contestId)
                val allClasses = contestRepository.getBonsaiClasses()
                val classIds = contestRepository.getContestClassIds(contestId)
                val classes = allClasses.filter { it.id in classIds }
                val allUsers = contestRepository.getUsers()
                mutableState.update {
                    it.copy(
                        contestClasses = contestClasses,
                        existingUsers = existingUsers,
                        classes = classes,
                        allUsers = allUsers,
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

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            mutableState.update { it.copy(suggestions = emptyList()) }
            return
        }
        searchJob = screenModelScope.launch {
            delay(300)
            val results = state.value.allUsers.filter {
                it.email.contains(query, ignoreCase = true)
            }
            mutableState.update { it.copy(suggestions = results) }
        }
    }

    fun clearSuggestions() {
        mutableState.update { it.copy(suggestions = emptyList()) }
    }

    fun assignJudge(email: String, classId: String, role: String = "judge") {
        screenModelScope.launch {
            mutableState.update { it.copy(isAssigning = true, error = null) }
            try {
                val userId = contestRepository.findUserIdByEmail(email)
                if (userId == null) {
                    mutableState.update {
                        it.copy(
                            isAssigning = false,
                            error = "User dengan email $email tidak ditemukan",
                        )
                    }
                    return@launch
                }
                val contestClassId = state.value.contestClasses
                    .firstOrNull { it.class_id == classId }?.id ?: return@launch
                contestRepository.assignJudge(contestId, userId, contestClassId, role)
                mutableState.update { it.copy(isAssigning = false) }
                loadData()
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isAssigning = false,
                        error = e.message ?: "Gagal menambah juri",
                    )
                }
            }
        }
    }

    fun removeJudge(userId: String) {
        screenModelScope.launch {
            mutableState.update { it.copy(error = null) }
            try {
                contestRepository.removeJudge(contestId, userId)
                loadData()
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(error = e.message ?: "Gagal menghapus juri")
                }
            }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    data class State(
        val contestClasses: List<BonsaiContestClass> = emptyList(),
        val existingUsers: List<ContestUser> = emptyList(),
        val allUsers: List<ContestUser> = emptyList(),
        val suggestions: List<ContestUser> = emptyList(),
        val classes: List<BonsaiClass> = emptyList(),
        val isLoading: Boolean = false,
        val isAssigning: Boolean = false,
        val error: String? = null,
    )
}

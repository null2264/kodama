package kodama.ui.presentation.contest.slop

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateContestScreenModel(
    private val contestRepository: ContestRepository,
    private val imageRepository: ImageRepository,
) : StateScreenModel<CreateContestScreenModel.State>(State()) {

    private var bannerBytes: ByteArray? = null

    init {
        screenModelScope.launch {
            try {
                val classes = contestRepository.getBonsaiClasses()
                mutableState.update { it.copy(availableClasses = classes) }
            } catch (_: Exception) {
            }
        }
    }

    fun onNameChanged(name: String) {
        mutableState.update { it.copy(name = name) }
    }

    fun onDescriptionChanged(description: String) {
        mutableState.update { it.copy(description = description) }
    }

    fun toggleClass(classId: String) {
        mutableState.update { current ->
            val newIds = if (classId in current.selectedClassIds) {
                current.selectedClassIds - classId
            } else {
                current.selectedClassIds + classId
            }
            current.copy(selectedClassIds = newIds)
        }
    }

    fun onBannerPicked(bytes: ByteArray) {
        bannerBytes = bytes
        mutableState.update { it.copy(bannerPreviewBytes = bytes) }
    }

    fun createContest(
        onError: (String) -> Unit = {},
        onSuccess: (contestId: String) -> Unit,
    ) {
        val currentState = state.value
        if (currentState.name.isBlank()) {
            onError("Nama lomba harus diisi")
            return
        }
        if (currentState.selectedClassIds.isEmpty()) {
            onError("Pilih minimal satu kelas bonsai")
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contestId = contestRepository.createContest(
                    name = currentState.name,
                    description = currentState.description.ifBlank { null },
                )
                contestRepository.addContestClasses(contestId, currentState.selectedClassIds)

                val bytes = bannerBytes
                if (bytes != null) {
                    val path = imageRepository.uploadContestBanner(contestId, bytes)
                    contestRepository.editContestBanner(contestId, path)
                }

                mutableState.update { it.copy(isLoading = false) }
                onSuccess(contestId)
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val name: String = "",
        val description: String = "",
        val selectedClassIds: List<String> = emptyList(),
        val availableClasses: List<BonsaiClass> = emptyList(),
        val bannerPreviewBytes: ByteArray? = null,
        val isLoading: Boolean = false,
    )
}

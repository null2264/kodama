package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditContestScreenModel(
    private val contestRepository: ContestRepository,
    private val imageRepository: ImageRepository,
    private val contestId: String,
) : StateScreenModel<EditContestScreenModel.State>(State()) {

    private var bannerBytes: ByteArray? = null
    private var existingBannerPath: String? = null

    init {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val contest = contestRepository.getContestById(contestId)
                val classIds = contestRepository.getContestClassIds(contestId)
                val availableClasses = contestRepository.getBonsaiClasses()
                existingBannerPath = contest?.banner_path
                mutableState.update {
                    it.copy(
                        name = contest?.name ?: "",
                        description = contest?.description ?: "",
                        selectedClassIds = classIds,
                        availableClasses = availableClasses,
                        bannerPath = contest?.banner_path,
                        bannerPreviewBytes = null,
                        isLoading = false,
                    )
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false) }
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

    fun saveContest(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
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
                contestRepository.updateContest(
                    contestId = contestId,
                    name = currentState.name,
                    description = currentState.description.ifBlank { null },
                )

                contestRepository.removeContestClasses(contestId)
                contestRepository.addContestClasses(contestId, currentState.selectedClassIds)

                val bytes = bannerBytes
                if (bytes != null) {
                    val path = imageRepository.uploadContestBanner(contestId, bytes)
                    contestRepository.editContestBanner(contestId, path)
                }

                mutableState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun deleteContest(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val bannerPath = existingBannerPath
                if (bannerPath != null) {
                    imageRepository.deleteImage(bannerPath)
                }
                contestRepository.deleteContest(contestId)
                mutableState.update { it.copy(isLoading = false) }
                onSuccess()
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
        val bannerPath: String? = null,
        val bannerPreviewBytes: ByteArray? = null,
        val isLoading: Boolean = false,
    )
}

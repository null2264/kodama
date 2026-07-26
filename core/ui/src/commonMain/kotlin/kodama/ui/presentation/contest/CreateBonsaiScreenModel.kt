package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
import kodama.core.data.BonsaiContestClass
import kodama.core.data.ContestClass
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateBonsaiScreenModel(
    private val contestRepository: ContestRepository,
    private val imageRepository: ImageRepository,
    private val contestId: String,
    private val availableClassIds: List<String>,
) : StateScreenModel<CreateBonsaiScreenModel.State>(State()) {

    private var imageBytes: ByteArray? = null

    init {
        screenModelScope.launch {
            try {
                val allClasses = contestRepository.getBonsaiContestClasses(contestId)
                val classes = allClasses.filter { it.class_id in availableClassIds }
                mutableState.update { it.copy(availableClasses = classes) }
            } catch (_: Exception) {
            }
        }
    }

    fun onNameChanged(name: String) {
        mutableState.update { it.copy(name = name) }
    }

    fun onClassSelected(classId: String) {
        mutableState.update { it.copy(selectedClassId = classId) }
    }

    fun onImagePicked(bytes: ByteArray) {
        imageBytes = bytes
        mutableState.update { it.copy(imagePreviewBytes = bytes) }
    }

    fun createBonsai(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        val currentState = state.value
        if (currentState.name.isBlank()) {
            onError("Nama bonsai harus diisi")
            return
        }
        if (currentState.selectedClassId == null) {
            onError("Pilih kelas bonsai")
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val isDuplicate = contestRepository.checkDuplicateBonsaiName(contestId, currentState.name)
                if (isDuplicate) {
                    mutableState.update { it.copy(isLoading = false) }
                    onError("Nama bonsai sudah digunakan dalam kontes ini")
                    return@launch
                }

                val bonsaiId = contestRepository.createBonsai(
                    contestId = contestId,
                    classId = currentState.selectedClassId,
                    name = currentState.name,
                )

                val bytes = imageBytes
                if (bytes != null) {
                    val path = imageRepository.uploadBonsaiPict(bonsaiId, bytes)
                    contestRepository.setBonsaiPictPath(bonsaiId, path)
                }

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
        val selectedClassId: String? = null,
        val availableClasses: List<BonsaiContestClass> = emptyList(),
        val imagePreviewBytes: ByteArray? = null,
        val isLoading: Boolean = false,
    )
}

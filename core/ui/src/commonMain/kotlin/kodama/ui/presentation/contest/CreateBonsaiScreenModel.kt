package kodama.ui.presentation.contest

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.BonsaiClass
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
    private var paymentProofBytes: ByteArray? = null
    private var paymentProofContentType: String? = null

    init {
        screenModelScope.launch {
            try {
                val allClasses = contestRepository.getBonsaiClasses()
                val classes = allClasses.filter { it.id in availableClassIds }
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

    fun onPaymentProofPicked(bytes: ByteArray, fileName: String, contentType: String) {
        paymentProofBytes = bytes
        paymentProofContentType = contentType
        mutableState.update { it.copy(paymentProofFileName = fileName) }
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
        if (paymentProofBytes == null) {
            onError("Bukti bayar wajib diunggah")
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
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

                val proofBytes = paymentProofBytes
                val proofContentType = paymentProofContentType
                if (proofBytes != null && proofContentType != null) {
                    val proofPath = imageRepository.uploadBonsaiProof(bonsaiId, proofBytes, proofContentType)
                    contestRepository.setBonsaiPaymentProofPath(bonsaiId, proofPath)
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
        val availableClasses: List<BonsaiClass> = emptyList(),
        val imagePreviewBytes: ByteArray? = null,
        val paymentProofFileName: String? = null,
        val isLoading: Boolean = false,
    )
}

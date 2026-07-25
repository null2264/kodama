package kodama.ui.presentation.image

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.ImageRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImageUploaderScreenModel(
    private val imageRepository: ImageRepository,
) : StateScreenModel<ImageUploaderScreenModel.State>(State()) {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L // 5MB
    }

    fun uploadBonsaiPict(bonsaiId: String, bytes: ByteArray) {
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            mutableState.update {
                it.copy(error = "File size must be less than 5MB")
            }
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isUploading = true, error = null) }
            try {
                val path = imageRepository.uploadBonsaiPict(bonsaiId, bytes)
                mutableState.update {
                    it.copy(
                        isUploading = false,
                        uploadedPath = path,
                        successMessage = "Image uploaded successfully"
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isUploading = false,
                        error = e.message ?: "Failed to upload image"
                    )
                }
            }
        }
    }

    fun uploadContestBanner(contestId: String, bytes: ByteArray) {
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            mutableState.update {
                it.copy(error = "File size must be less than 5MB")
            }
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isUploading = true, error = null) }
            try {
                val path = imageRepository.uploadContestBanner(contestId, bytes)
                mutableState.update {
                    it.copy(
                        isUploading = false,
                        uploadedPath = path,
                        successMessage = "Banner uploaded successfully"
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isUploading = false,
                        error = e.message ?: "Failed to upload banner"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        mutableState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    data class State(
        val isUploading: Boolean = false,
        val uploadedPath: String? = null,
        val error: String? = null,
        val successMessage: String? = null,
    )
}

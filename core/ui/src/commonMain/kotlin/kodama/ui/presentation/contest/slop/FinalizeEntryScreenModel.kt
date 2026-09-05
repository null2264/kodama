package kodama.ui.presentation.contest.slop

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinalizeEntryScreenModel(
    private val contestRepository: ContestRepository,
    private val imageRepository: ImageRepository,
    private val contestId: String,
    private val bonsaiId: String,
) : StateScreenModel<FinalizeEntryScreenModel.State>(State()) {

    private var receiptBytes: ByteArray? = null
    private var receiptContentType: String? = null

    fun onReceiptPicked(bytes: ByteArray, fileName: String, contentType: String) {
        receiptBytes = bytes
        receiptContentType = contentType
        mutableState.update { it.copy(receiptFileName = fileName) }
    }

    fun finalizeEntry(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        val proofBytes = receiptBytes
        val proofContentType = receiptContentType
        if (proofBytes == null || proofContentType == null) {
            onError("Bukti bayar wajib diunggah")
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isFinalizing = true) }
            try {
                val proofPath = imageRepository.uploadBonsaiProof(bonsaiId, proofBytes, proofContentType)
                contestRepository.setBonsaiPaymentProofPath(bonsaiId, proofPath)
                contestRepository.finalizeBonsai(bonsaiId)
                mutableState.update { it.copy(isFinalizing = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isFinalizing = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val receiptFileName: String? = null,
        val isFinalizing: Boolean = false,
    )
}

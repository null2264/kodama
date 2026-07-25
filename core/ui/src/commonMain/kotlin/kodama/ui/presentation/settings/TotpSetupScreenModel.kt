package kodama.ui.presentation.settings

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TotpSetupScreenModel(
    private val auth: Auth,
) : StateScreenModel<TotpSetupScreenModel.State>(State()) {

    init {
        enroll()
    }

    private fun enroll() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val factor = auth.mfa.enroll(factorType = FactorType.TOTP) {
                    issuer = "Kodama"
                }
                val (factorId, _, qrCode) = factor.data
                mutableState.update {
                    it.copy(
                        factorId = factorId,
                        qrCode = qrCode,
                        isLoading = false,
                    )
                }
            } catch (err: AuthRestException) {
                mutableState.update {
                    it.copy(error = err.errorDescription, isLoading = false)
                }
            }
        }
    }

    fun onCodeChanged(code: String) {
        mutableState.update { it.copy(code = code) }
    }

    fun verify(onError: (AuthRestException) -> Unit = {}, onSuccess: () -> Unit = {}) {
        val factorId = state.value.factorId ?: return
        screenModelScope.launch {
            mutableState.update { it.copy(isVerifying = true) }
            try {
                auth.mfa.createChallengeAndVerify(
                    factorId = factorId,
                    code = state.value.code,
                )
                onSuccess()
            } catch (err: AuthRestException) {
                onError(err)
            } finally {
                mutableState.update { it.copy(isVerifying = false) }
            }
        }
    }

    data class State(
        val factorId: String? = null,
        val qrCode: String? = null,
        val code: String = "",
        val isLoading: Boolean = false,
        val isVerifying: Boolean = false,
        val error: String? = null,
    )
}

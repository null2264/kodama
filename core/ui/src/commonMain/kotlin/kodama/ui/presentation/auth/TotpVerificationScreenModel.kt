package kodama.ui.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TotpVerificationScreenModel(
    private val auth: Auth,
    private val factorId: String,
    private val challengeId: String,
) : StateScreenModel<TotpVerificationScreenModel.State>(State()) {

    fun onTotpChanged(totp: String) {
        mutableState.update { it.copy(totp = totp) }
    }

    fun verify(onError: (AuthRestException) -> Unit = {}, onSuccess: () -> Unit = {}) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                auth.mfa.verifyChallenge(
                    factorId = factorId,
                    challengeId = challengeId,
                    code = state.value.totp,
                )
                onSuccess()
            } catch (err: AuthRestException) {
                onError(err)
            } finally {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    data class State(
        val totp: String = "",
        val isLoading: Boolean = false,
    )
}

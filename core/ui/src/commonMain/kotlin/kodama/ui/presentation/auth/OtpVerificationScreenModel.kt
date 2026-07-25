package kodama.ui.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpVerificationScreenModel(
    private val auth: Auth,
    private val email: String,
) : StateScreenModel<OtpVerificationScreenModel.State>(State()) {

    fun onOtpChanged(otp: String) {
        mutableState.update { it.copy(otp = otp) }
    }

    fun verify(onError: (AuthRestException) -> Unit = {}, onSuccess: () -> Unit = {}) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = email,
                    token = state.value.otp,
                )
                onSuccess()
            } catch (err: AuthRestException) {
                onError(err)
            } finally {
                mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resendOtp(onError: (AuthRestException) -> Unit = {}, onSuccess: () -> Unit = {}) {
        screenModelScope.launch {
            mutableState.update { it.copy(isResending = true) }
            try {
                auth.signInWith(OTP) {
                    this.email = this@OtpVerificationScreenModel.email
                }
                onSuccess()
            } catch (err: AuthRestException) {
                onError(err)
            } finally {
                mutableState.update { it.copy(isResending = false) }
            }
        }
    }

    data class State(
        val otp: String = "",
        val isLoading: Boolean = false,
        val isResending: Boolean = false,
    )
}

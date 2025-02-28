package kodama.ui.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthScreenModel(private val auth: Auth) : StateScreenModel<AuthScreenModel.State>(State()) {

    fun toggleAuth() {
        mutableState.update {
            it.copy(signUp = !it.signUp)
        }
    }

    fun onEmailFieldChanged(email: String) {
        mutableState.update {
            it.copy(email = email)
        }
    }

    fun onPasswordFieldChanged(password: String) {
        mutableState.update {
            it.copy(password = password)
        }
    }

    fun authenticate() {
        screenModelScope.launch {
            val isSignUp = state.value.signUp
            if (isSignUp) {
                auth.signUpWith(Email) {
                    this.email = state.value.email
                    this.password = state.value.password
                }
            } else {
                auth.signInWith(Email) {
                    this.email = state.value.email
                    this.password = state.value.password
                }
            }
        }
    }

    fun verify(otp: String) {
        screenModelScope.launch {
            auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = state.value.email,
                token = otp,
            )
        }
    }

    data class State(
        val signUp: Boolean = false,
        val email: String = "",
        val password: String = "",
    )
}

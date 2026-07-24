package kodama.ui.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthScreenModel(private val auth: Auth) : StateScreenModel<AuthScreenModel.State>(State()) {

    fun toggleAuth() {
        mutableState.update {
            it.copy(signUp = !it.signUp)
        }
    }

    fun onUsernameFieldChanged(username: String) {
        mutableState.update {
            it.copy(username = username)
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

    fun authenticate(onError: (AuthRestException) -> Unit = {}) {
        screenModelScope.launch {
            val isSignUp = state.value.signUp
            if (isSignUp) {
                try {
                    auth.signUpWith(Email) {
                        this.email = state.value.email
                        this.password = state.value.password
                        this.data = buildJsonObject {
                            put("name", state.value.username)
                        }
                    }
                } catch (err: AuthRestException) {
                    onError(err)
                }
            } else {
                try {
                    auth.signInWith(Email) {
                        this.email = state.value.email
                        this.password = state.value.password
                    }
                } catch (err: AuthRestException) {
                    onError(err)
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
        val username: String = "",  // Used for sign up only
        val email: String = "",
        val password: String = "",
    )
}

package kodama.ui.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginScreenModel(private val auth: Auth) : StateScreenModel<LoginScreenModel.State>(State()) {
    fun toggleAuth() {
        mutableState.update {
            it.copy(
                signUp = !it.signUp,
            )
        }
    }

    fun authenticate(email: String, password: String) {
        screenModelScope.launch {
            val isSignUp = state.value.signUp
            if (isSignUp) {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
        }
    }

    data class State(
        val signUp: Boolean = false,
    )
}

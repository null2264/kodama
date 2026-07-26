package kodama.ui.presentation.profile

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EditProfileScreenModel(
    private val auth: Auth,
) : StateScreenModel<EditProfileScreenModel.State>(State()) {

    init {
        val user = auth.currentUserOrNull()
        mutableState.update {
            it.copy(
                name = user?.userMetadata?.get("name")?.toString()?.trim('"') ?: "",
                email = user?.email ?: "",
            )
        }
    }

    fun onNameChanged(name: String) {
        mutableState.update { it.copy(name = name) }
    }

    fun onEmailChanged(email: String) {
        mutableState.update { it.copy(email = email) }
    }

    fun saveProfile(
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        val currentState = state.value
        if (currentState.name.isBlank()) {
            onError("Nama harus diisi")
            return
        }
        if (currentState.email.isBlank() || !currentState.email.contains("@")) {
            onError("Email tidak valid")
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            try {
                auth.updateUser {
                    data = buildJsonObject {
                        put("name", currentState.name)
                    }
                }
                val currentEmail = auth.currentUserOrNull()?.email
                if (currentState.email != currentEmail) {
                    auth.updateUser {
                        email = currentState.email
                    }
                }
                mutableState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                mutableState.update { it.copy(isSaving = false) }
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    data class State(
        val name: String = "",
        val email: String = "",
        val isSaving: Boolean = false,
    )
}

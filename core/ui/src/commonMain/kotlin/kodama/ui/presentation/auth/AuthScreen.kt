package kodama.ui.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.compose.auth.ui.annotations.AuthUiExperimental
import io.github.jan.supabase.compose.auth.ui.password.PasswordField
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel

internal object AuthScreen : Screen() {
    @OptIn(AuthUiExperimental::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<AuthScreenModel>()

        val state by screenModel.state.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val passwordFocus = remember { FocusRequester() }
            OutlinedTextField(
                value = state.email,
                onValueChange = screenModel::onEmailFieldChanged,
                singleLine = true,
                label = { Text("E-Mail") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                leadingIcon = { Icon(Icons.Filled.Email, "Email") },
            )
            PasswordField(
                value = state.password,
                onValueChange = screenModel::onPasswordFieldChanged,
                modifier = Modifier.focusRequester(passwordFocus)
                    .padding(top = 10.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    screenModel.authenticate()
                }),
            )
            Button(
                onClick = { screenModel.authenticate() },
                modifier = Modifier.padding(top = 10.dp),
                enabled = state.email.isNotBlank() && state.password.isNotBlank()
            ) {
                Text(if (state.signUp) "Register" else "Login")
            }
//        GoogleButton(
//            text = if (signUp) "Sign Up with Google" else "Login with Google"
//        ) { viewModel.loginWithGoogle() }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            TextButton(onClick = { screenModel.toggleAuth() }) {
                Text(if (state.signUp) "Already have an account? Login" else "Not registered? Register")
            }
        }
    }
}

package kodama.ui.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.compose.auth.ui.annotations.AuthUiExperimental
import io.github.jan.supabase.compose.auth.ui.password.PasswordField
import kodama.resources.Overpass_VariableFont
import kodama.resources.Res
import kodama.resources.icons.alternate_email
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.KodamaTextField
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.Font

internal class AuthScreen : Screen() {
    @OptIn(AuthUiExperimental::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<AuthScreenModel>()

        val state by screenModel.state.collectAsState()

        var alertDialog: AlertDialogBuilder? by remember { mutableStateOf(null) }

        KodamaScaffold(
            onNavigationIconClicked = {},
            appBarType = AppBarType.NONE,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    Text(
                        text = if (state.signUp) "Daftar" else "Selamat Datang",
                        fontFamily = FontFamily(Font(Res.font.Overpass_VariableFont)),
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.W900,
                        ),
                    )
                    Text(
                        text = if (state.signUp) "Silahkan isi formulir di bawah ini" else "Silahkan masuk",
                        fontFamily = FontFamily(Font(Res.font.Overpass_VariableFont)),
                        style = TextStyle(
                            fontSize = 16.sp,
                        ),
                    )
                }
                val passwordFocus = remember { FocusRequester() }
                val emailFocus = remember { FocusRequester() }

                var isNameError by remember { mutableStateOf(false) }
                var isEmailError by remember { mutableStateOf(false) }
                var isPasswordError by remember { mutableStateOf(false) }

                if (state.signUp) {
                    KodamaTextField(
                        value = state.username,
                        onValueChange = {
                            screenModel.onUsernameFieldChanged(it)
                            isNameError = it.isEmpty()
                        },
                        singleLine = true,
                        label = "Name",
                        placeholder = "John Doe",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { emailFocus.requestFocus() }),
                        icon = { Icon(alternate_email, "Email") },
                        isError = isNameError,
                    )
                }
                KodamaTextField(
                    value = state.email,
                    onValueChange = {
                        screenModel.onEmailFieldChanged(it)
                        isEmailError = it.isEmpty() || !it.contains('@')
                    },
                    singleLine = true,
                    label = "Email",
                    placeholder = "contoh@email.co.id",
                    modifier = Modifier.focusRequester(emailFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                    icon = { Icon(alternate_email, "Email") },
                    isError = isEmailError,
                )
                KodamaTextField(
                    value = state.password,
                    onValueChange = {
                        screenModel.onPasswordFieldChanged(it)
                        isPasswordError = it.isEmpty() || it.count() < 8
                    },
                    label = "Password",
                    placeholder = "Minimal 8 karakter",
                    modifier = Modifier.focusRequester(passwordFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            screenModel.authenticate(onError = { err ->
                                alertDialog = AlertDialogBuilder().apply {
                                    title = "Something went wrong!"
                                    text = err.errorDescription
                                    onConfirm = { alertDialog = null }
                                    onCancel = { alertDialog = null }
                                }
                            })
                        }
                    ),
                    icon = { Icon(alternate_email, "Email") },
                    isPassword = true,
                    isError = isPasswordError,
                )
                Button(
                    onClick = {
                        screenModel.authenticate(onError = { err ->
                            alertDialog = AlertDialogBuilder().apply {
                                title = "Something went wrong!"
                                text = err.errorDescription
                                onConfirm = { alertDialog = null }
                                onCancel = { alertDialog = null }
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    enabled =
                        state.email.isNotBlank() &&
                            !isEmailError &&
                            state.password.isNotBlank() &&
                            !isPasswordError &&
                            ((state.username.isNotBlank() && !isNameError) || !state.signUp)
                ) {
                    Text(if (state.signUp) "Register" else "Login")
                }
//        GoogleButton(
//            text = if (signUp) "Sign Up with Google" else "Login with Google"
//        ) { viewModel.loginWithGoogle() }
            }
            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
                TextButton(onClick = { screenModel.toggleAuth() }) {
                    Text(if (state.signUp) "Already have an account? Login" else "Not registered? Register")
                }
            }

            alertDialog?.build()
        }
    }
}

package kodama.ui.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kodama.core.util.OperatingSystem
import kodama.core.util.getCurrentOS
import kodama.resources.Overpass_VariableFont
import kodama.resources.Res
import kodama.resources.error_something_wrong
import kodama.resources.icons.alternate_email
import kodama.resources.totp_hint
import kodama.resources.totp_subtitle
import kodama.resources.totp_verification
import kodama.resources.totp_verify
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.KodamaTextField
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

internal class TotpVerificationScreen(
    private val factorId: String,
    private val challengeId: String,
) : Screen() {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<TotpVerificationScreenModel> {
            org.koin.core.parameter.parametersOf(factorId, challengeId)
        }
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
                        text = stringResource(Res.string.totp_verification),
                        fontFamily = FontFamily(Font(Res.font.Overpass_VariableFont)),
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.W900,
                        ),
                    )
                    Text(
                        text = stringResource(Res.string.totp_subtitle),
                        fontFamily = FontFamily(Font(Res.font.Overpass_VariableFont)),
                        style = TextStyle(
                            fontSize = 16.sp,
                        ),
                    )
                }

                val totpFocus = remember { FocusRequester() }
                var isTotpError by remember { mutableStateOf(false) }

                val keyboardController = if (getCurrentOS() == OperatingSystem.ANDROID) LocalSoftwareKeyboardController.current else null
                val verify = {
                    keyboardController?.hide()
                    screenModel.verify(
                        onError = { err ->
                            alertDialog = AlertDialogBuilder().apply {
                                titleRes = Res.string.error_something_wrong
                                text = err.errorDescription
                                onConfirm = { alertDialog = null }
                                onCancel = { alertDialog = null }
                            }
                        }
                    )
                }

                KodamaTextField(
                    value = state.totp,
                    onValueChange = {
                        screenModel.onTotpChanged(it)
                        isTotpError = it.length != 6
                    },
                    singleLine = true,
                    label = stringResource(Res.string.totp_hint),
                    placeholder = "123456",
                    modifier = Modifier.focusRequester(totpFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { verify() }),
                    icon = { Icon(alternate_email, "TOTP") },
                    isError = isTotpError,
                )

                LoadingButton(
                    onClick = { verify() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    isLoading = state.isLoading,
                    enabled = state.totp.length == 6 && !isTotpError,
                ) {
                    Text(stringResource(Res.string.totp_verify))
                }
            }

            alertDialog?.build()
        }
    }
}

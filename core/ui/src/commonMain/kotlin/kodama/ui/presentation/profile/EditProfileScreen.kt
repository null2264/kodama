package kodama.ui.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import kodama.resources.Res
import kodama.resources.icons.alternate_email
import kodama.resources.icons.account_circle
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.KodamaTextField
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.stringResource

internal class EditProfileScreen : Screen() {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<EditProfileScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current

        var alertDialogMessage by remember { mutableStateOf<String?>(null) }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = "Edit Profile",
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = account_circle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(80.dp),
                    )
                    LoadingButton(
                        onClick = { },
                        modifier = Modifier.padding(top = 8.dp),
                        isLoading = false,
                        enabled = false,
                    ) {
                        Text("Upload")
                    }
                }

                KodamaTextField(
                    value = state.name,
                    onValueChange = { screenModel.onNameChanged(it) },
                    label = "Name",
                    placeholder = "John Doe",
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    icon = { Icon(account_circle, contentDescription = null, modifier = Modifier.size(24.dp)) },
                )

                KodamaTextField(
                    value = state.email,
                    onValueChange = { screenModel.onEmailChanged(it) },
                    label = "Email Address",
                    placeholder = "johndoe@example.org",
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    icon = { Icon(alternate_email, contentDescription = null, modifier = Modifier.size(24.dp)) },
                )

                LoadingButton(
                    onClick = {
                        screenModel.saveProfile(
                            onError = { alertDialogMessage = it },
                            onSuccess = { navigator?.pop() },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    isLoading = state.isSaving,
                    enabled = state.name.isNotBlank() && state.email.isNotBlank() && !state.isSaving,
                ) {
                    Text("Save")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            alertDialogMessage?.let { message ->
                kodama.ui.component.AlertDialogBuilder().apply {
                    title = "Terjadi kesalahan!"
                    text = message
                    onConfirm = { alertDialogMessage = null }
                    onCancel = { alertDialogMessage = null }
                }.build()
            }
        }
    }
}

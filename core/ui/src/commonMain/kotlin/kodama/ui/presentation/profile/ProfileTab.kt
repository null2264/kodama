package kodama.ui.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.github.jan.supabase.auth.Auth
import kodama.resources.Res
import kodama.resources.icons.account_circle
import kodama.resources.icons.arrow_back
import kodama.resources.icons.edit
import kodama.resources.logout
import kodama.resources.security_settings
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.settings.TotpSetupScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal object ProfileTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(account_circle)

            return remember {
                TabOptions(
                    index = 2u,
                    title = "You",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.current

        val auth: Auth = koinInject()
        val user = auth.currentUserOrNull()
        val userName = user?.userMetadata?.get("name")?.toString()?.trim('"') ?: "unnamed"
        val userEmail = user?.email ?: ""

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = account_circle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigator?.push(EditProfileScreen()) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = edit,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = arrow_back,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }

            HorizontalDivider()

            Text(
                text = "Security",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigator?.push(TotpSetupScreen()) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = account_circle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(Res.string.security_settings),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = arrow_back,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            var isLoggingOut by remember { mutableStateOf(false) }

            LoadingButton(
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoggingOut,
                onClick = {
                    isLoggingOut = true
                    coroutineScope.launch {
                        try {
                            auth.signOut()
                        } catch (_: Exception) {
                            isLoggingOut = false
                        }
                    }
                },
            ) {
                Text(stringResource(Res.string.logout))
            }
        }
    }
}

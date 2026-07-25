package kodama.ui.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
                    title = "Profile",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.current

        val auth: Auth = koinInject()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(auth.currentUserOrNull()?.userMetadata?.get("name")?.toString() ?: "unnamed")

            var isLoggingOut by remember { mutableStateOf(false) }

            LoadingButton(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
                isLoading = false,
                onClick = { navigator?.push(TotpSetupScreen()) },
            ) {
                Text(stringResource(Res.string.security_settings))
            }

            LoadingButton(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
                isLoading = isLoggingOut,
                onClick = {
                    isLoggingOut = true
                    coroutineScope.launch { auth.signOut() }
                },
            ) {
                Text(stringResource(Res.string.logout))
            }
        }
    }
}

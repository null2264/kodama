package kodama.ui.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
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
import kodama.ui.presentation.main.MainViewModel
import kodama.ui.presentation.settings.TotpSetupScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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

        val scrollState = rememberScrollState()
        val mainViewModel = koinViewModel<MainViewModel>()

        LaunchedEffect(Unit) {
            mainViewModel.updateScrollBehaviour(
                scrollState.value == 0,
                scrollState.canScrollForward || scrollState.canScrollBackward,
            )
        }

        LaunchedEffect(scrollState) {
            combine(
                snapshotFlow { scrollState.value == 0 },
                snapshotFlow { scrollState.canScrollForward || scrollState.canScrollBackward },
            ) {
                Pair(it[0], it[1])
            }
                .distinctUntilChanged()
                .collect { (atTop, canScroll) ->
                    mainViewModel.updateScrollBehaviour(atTop, canScroll)
                }
        }

        val auth: Auth = koinInject()
        val user = auth.currentUserOrNull()
        val userName = user?.userMetadata?.get("name")?.toString()?.trim('"') ?: "unnamed"
        val userEmail = user?.email ?: ""

        var isLoggingOut by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            item {
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
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            val generalOptions = listOf(
                Option("Edit Profile", edit) { it.push(EditProfileScreen()) },
                Option("Appearance", edit) {},
            )
            itemsIndexed(generalOptions) { index, option ->
                option.OptionCard(index, generalOptions)
            }

            item {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }

            val securityOptions = listOf(
                Option("Keamanan", account_circle) { it.push(TotpSetupScreen()) },
            )
            itemsIndexed(securityOptions) { index, option ->
                option.OptionCard(index, securityOptions)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
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
}

@Composable
fun Option.OptionCard(index: Int, optionList: List<Option>) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(when {
                index <= 0 -> 2.dp
                else -> 0.dp
            })
            .clickable() {},
        shape = when {
            optionList.size == 1 -> RoundedCornerShape(16.dp)
            index <= 0 -> RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 16.dp, topEnd = 16.dp)
            index >= optionList.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)
            else -> RoundedCornerShape(4.dp)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

data class Option(val name: String, val icon: ImageVector, val onNavigate: (Navigator) -> Unit)

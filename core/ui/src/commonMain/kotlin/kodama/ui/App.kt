package kodama.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.CurrentScreen
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.presentation.auth.AuthScreen
import kodama.ui.presentation.contest.BonsaiDetailScreen
import kodama.ui.presentation.main.MainScreen
import kodama.ui.presentation.utils.NotAuthenticatedScreen
import org.koin.compose.koinInject

data class DeepLinkParams(
    val contestId: String,
    val bonsaiId: String,
)

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
    deepLinkParams: DeepLinkParams? = null,
    onReady: () -> Unit = {},
) {
    val status by supabaseAuth.sessionStatus.collectAsState()
    val isAuthenticated = status is SessionStatus.Authenticated

    val initialScreen = remember(isAuthenticated) {
        when (isAuthenticated) {
            true -> MainScreen()
            false -> AuthScreen()
        }
    }

    Navigator(initialScreen) { navigator ->
        LaunchedEffect(isAuthenticated) {
            when {
                isAuthenticated -> {
                    val lastItem = navigator.lastItemOrNull
                    if (lastItem == null || lastItem is NotAuthenticatedScreen) {
                        navigator.replace(MainScreen())
                    }
                    onReady()
                }
                status is SessionStatus.NotAuthenticated || status is SessionStatus.RefreshFailure -> {
                    if (navigator.lastItem !is NotAuthenticatedScreen) {
                        navigator.replaceAll(AuthScreen())
                    }
                    onReady()
                }
                status is SessionStatus.Initializing -> {}
            }
        }

        LaunchedEffect(deepLinkParams) {
            if (deepLinkParams != null && status is SessionStatus.Authenticated) {
                navigator.push(BonsaiDetailScreen(deepLinkParams.contestId, deepLinkParams.bonsaiId))
            }
        }

        CurrentScreen()
    }
}

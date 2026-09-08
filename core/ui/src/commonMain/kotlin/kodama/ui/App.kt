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
import kodama.ui.presentation.bonsai.BonsaiDetailScreen
import kodama.ui.presentation.main.EmptyScreen
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
    clearDeepLink: () -> Unit = {},
    onReady: () -> Unit = {},
) {
    val status by supabaseAuth.sessionStatus.collectAsState()
    val isAuthenticated = status is SessionStatus.Authenticated

    val initialScreen = remember(status) {
        when(status) {
            is SessionStatus.Authenticated -> MainScreen()
            is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> AuthScreen()
            is SessionStatus.Initializing -> EmptyScreen
        }
    }

    Navigator(initialScreen) { navigator ->
        LaunchedEffect(status) {
            when(status) {
                is SessionStatus.Authenticated -> {
                    val lastItem = navigator.lastItemOrNull
                    if (lastItem == null || lastItem is NotAuthenticatedScreen) {
                        navigator.replace(MainScreen())
                    }
                    onReady()
                }
                is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> {
                    if (navigator.lastItem !is NotAuthenticatedScreen || navigator.lastItem is EmptyScreen) {
                        navigator.replaceAll(AuthScreen())
                    }
                    onReady()
                }
                is SessionStatus.Initializing -> {}
            }
        }

        LaunchedEffect(deepLinkParams, isAuthenticated) {
            if (deepLinkParams != null && isAuthenticated) {
                navigator.push(BonsaiDetailScreen(deepLinkParams.contestId, deepLinkParams.bonsaiId))
                clearDeepLink()
            }
        }

        CurrentScreen()
    }
}

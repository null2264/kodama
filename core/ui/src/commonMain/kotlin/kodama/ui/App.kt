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

    val initialScreen = remember(status) {
        when (status) {
            is SessionStatus.Authenticated -> MainScreen()
            else -> AuthScreen()
        }
    }

    Navigator(initialScreen) { navigator ->
        LaunchedEffect(status) {
            when (status) {
                is SessionStatus.Authenticated -> {
                    if (navigator.lastItem !is MainScreen) {
                        navigator.replace(MainScreen())
                    }
                    onReady()
                }
                is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> {
                    if (navigator.lastItem !is AuthScreen) {
                        navigator.replaceAll(AuthScreen())
                    }
                    onReady()
                }
                is SessionStatus.Initializing -> {}
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

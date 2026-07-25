package kodama.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.util.Platform
import kodama.core.util.OperatingSystem
import kodama.core.util.getCurrentOS
import kodama.ui.presentation.auth.AuthScreen
import kodama.ui.presentation.home.HomeScreen
import org.koin.compose.koinInject

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
) {
    val status by supabaseAuth.sessionStatus.collectAsState()

    if (status is SessionStatus.Initializing && getCurrentOS() == OperatingSystem.ANDROID) return

    when (status) {
        is SessionStatus.Initializing -> {
            // TODO: Splash screen for other OSes
        }
        is SessionStatus.Authenticated -> Navigator(HomeScreen())
        is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> Navigator(AuthScreen())
    }
}

package kodama.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.presentation.auth.AuthScreen
import kodama.ui.presentation.home.HomeScreen
import org.koin.compose.koinInject

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
) {
    val status by supabaseAuth.sessionStatus.collectAsState()

    when (status) {
        is SessionStatus.Authenticated -> Navigator(HomeScreen)
        else -> Navigator(AuthScreen)
    }
}

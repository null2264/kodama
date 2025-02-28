package kodama.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.presentation.auth.LoginScreen
import kodama.ui.presentation.home.HomeScreen
import org.koin.compose.koinInject

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
) {
    val status by supabaseAuth.sessionStatus.collectAsState()

    when (status) {
        is SessionStatus.Authenticated -> Navigator(HomeScreen)
        else -> Navigator(LoginScreen)
    }
}

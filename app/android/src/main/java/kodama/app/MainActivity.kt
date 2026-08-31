package kodama.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.App
import kodama.ui.DeepLinkParams
import kodama.ui.theme.KodamaTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val auth: Auth by inject()
    private var composableReady = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            auth.sessionStatus.value is SessionStatus.Initializing || !composableReady.value
        }

        val deepLinkParams = parseDeepLink(intent?.data)

        setContent {
            KodamaTheme { isDark ->
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                SideEffect {
                    enableEdgeToEdge(navigationBarStyle = if (isDark) darkStyle else lightStyle)
                }
                App(
                    deepLinkParams = deepLinkParams,
                    onReady = { composableReady.value = true },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val params = parseDeepLink(intent?.data)
        if (params != null) {
            // Handle deep link when app is already running
            // The App composable will pick this up on recomposition
            setIntent(intent)
        }
    }

    private fun parseDeepLink(uri: android.net.Uri?): DeepLinkParams? {
        if (uri == null) return null
        val scheme = uri.scheme ?: return null
        if (scheme != "kodama") return null

        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2) return null

        val contestId = pathSegments[0]
        val bonsaiId = pathSegments[1]

        if (contestId.isBlank() || bonsaiId.isBlank()) return null

        return DeepLinkParams(
            contestId = contestId,
            bonsaiId = bonsaiId,
        )
    }
}

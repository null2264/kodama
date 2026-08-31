package kodama.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.App
import kodama.ui.DeepLinkParams
import kodama.ui.theme.KodamaTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val auth: Auth by inject()
    private var composableReady by mutableStateOf(false)
    private var deepLinkParams by mutableStateOf<DeepLinkParams?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !(auth.sessionStatus.value !is SessionStatus.Initializing || composableReady)
        }

        deepLinkParams = parseDeepLink(intent?.data)

        setContent {
            KodamaTheme { isDark ->
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                SideEffect {
                    enableEdgeToEdge(navigationBarStyle = if (isDark) darkStyle else lightStyle)
                }
                App(
                    deepLinkParams = deepLinkParams,
                    onReady = { composableReady = true },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        deepLinkParams = parseDeepLink(intent?.data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun parseDeepLink(uri: android.net.Uri?): DeepLinkParams? {
        if (uri == null) return null
        val scheme = uri.scheme ?: return null
        if (scheme != "kodama") return null

        val contestId = uri.host ?: return null
        val bonsaiId = uri.pathSegments.firstOrNull() ?: return null

        if (contestId.isBlank() || bonsaiId.isBlank()) return null

        return DeepLinkParams(
            contestId = contestId,
            bonsaiId = bonsaiId,
        )
    }
}

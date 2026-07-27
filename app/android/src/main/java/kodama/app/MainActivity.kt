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

        setContent {
            KodamaTheme { isDark ->
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                SideEffect {
                    enableEdgeToEdge(navigationBarStyle = if (isDark) darkStyle else lightStyle)
                }
                App(onReady = { composableReady.value = true })
            }
        }
    }
}

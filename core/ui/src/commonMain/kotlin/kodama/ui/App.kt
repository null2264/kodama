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
import kodama.ui.presentation.main.MainScreen
import org.koin.compose.koinInject

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
    onReady: () -> Unit = {},
) {
    val status by supabaseAuth.sessionStatus.collectAsState()

    // FIXME: TOTP is a bit complicated to implement at the moment
//    try {
//        var totpFactorId: String? = null
//        var totpChallengeId: String? = null
//        val aal = supabaseAuth.mfa.getAuthenticatorAssuranceLevel()
//        if (aal.current == AuthenticatorAssuranceLevel.AAL1 && aal.next == AuthenticatorAssuranceLevel.AAL2) {
//            val factors = supabaseAuth.mfa.verifiedFactors
//            val totpFactor = factors.firstOrNull { it.factorType == FactorType.TOTP.value }
//            if (totpFactor != null) {
//                val challenge = supabaseAuth.mfa.createChallenge(totpFactor.id)
//                totpFactorId = totpFactor.id
//                totpChallengeId = challenge.id
//            }
//        }
//        navigator.replace(
//            if (totpFactorId != null && totpChallengeId != null)
//                TotpVerificationScreen(totpFactorId, totpChallengeId)
//            else
//                MainScreen()
//        )
//    } catch (_: Exception) {
//    } finally {
//        onReady()
//    }

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
        CurrentScreen()
    }
}

package kodama.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.ui.presentation.auth.AuthScreen
import kodama.ui.presentation.auth.TotpVerificationScreen
import kodama.ui.presentation.main.MainScreen
import org.koin.compose.koinInject

@Composable
fun App(
    supabaseAuth: Auth = koinInject(),
) {
    val status by supabaseAuth.sessionStatus.collectAsState()
    var needsTotp by remember { mutableStateOf(false) }
    var totpFactorId by remember { mutableStateOf<String?>(null) }
    var totpChallengeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(status) {
        if (status is SessionStatus.Authenticated) {
            try {
                val aal = supabaseAuth.mfa.getAuthenticatorAssuranceLevel()
                if (aal.current == AuthenticatorAssuranceLevel.AAL1 && aal.next == AuthenticatorAssuranceLevel.AAL2) {
                    val factors = supabaseAuth.mfa.retrieveFactorsForCurrentUser()
                    val totpFactor = factors.firstOrNull { it.factorType == FactorType.TOTP.value }
                    if (totpFactor != null) {
                        val challenge = supabaseAuth.mfa.createChallenge(totpFactor.id)
                        totpFactorId = totpFactor.id
                        totpChallengeId = challenge.id
                        needsTotp = true
                    }
                }
            } catch (_: Exception) {
                // MFA check failed, proceed without TOTP
            }
        }
    }

    if (status is SessionStatus.Initializing) return

    when {
        needsTotp && totpFactorId != null && totpChallengeId != null -> {
            Navigator(TotpVerificationScreen(totpFactorId!!, totpChallengeId!!))
        }
        status is SessionStatus.Authenticated && !needsTotp -> Navigator(MainScreen())
        status is SessionStatus.NotAuthenticated || status is SessionStatus.RefreshFailure -> Navigator(AuthScreen())
    }
}

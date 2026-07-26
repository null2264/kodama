package kodama.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.CurrentScreen
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
    onReady: () -> Unit = {},
) {
    val status by supabaseAuth.sessionStatus.collectAsState()
    var needsTotp by remember { mutableStateOf(false) }
    var totpFactorId by remember { mutableStateOf<String?>(null) }
    var totpChallengeId by remember { mutableStateOf<String?>(null) }
    var mfaChecked by remember { mutableStateOf(false) }

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
            }
            mfaChecked = true
        } else if (status is SessionStatus.NotAuthenticated || status is SessionStatus.RefreshFailure) {
            mfaChecked = true
        }
    }

    val initialScreen = when (status) {
        is SessionStatus.Authenticated -> MainScreen()
        else -> AuthScreen()
    }

    Navigator(initialScreen) { navigator ->
        LaunchedEffect(status, needsTotp, totpFactorId, totpChallengeId, mfaChecked) {
            when {
                status is SessionStatus.Initializing -> {}
                !mfaChecked -> {}
                needsTotp && totpFactorId != null && totpChallengeId != null -> {
                    navigator.replaceAll(TotpVerificationScreen(totpFactorId!!, totpChallengeId!!))
                    onReady()
                }
                status is SessionStatus.Authenticated && !needsTotp -> {
                    if (navigator.lastItem !is MainScreen) {
                        navigator.replaceAll(MainScreen())
                    }
                    onReady()
                }
                status is SessionStatus.NotAuthenticated || status is SessionStatus.RefreshFailure -> {
                    if (navigator.lastItem is MainScreen || navigator.lastItem is TotpVerificationScreen) {
                        navigator.replaceAll(AuthScreen())
                    }
                    onReady()
                }
            }
        }
        CurrentScreen()
    }
}

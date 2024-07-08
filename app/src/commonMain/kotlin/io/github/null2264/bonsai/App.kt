package io.github.null2264.bonsai

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import io.github.null2264.bonsai.core.di.appModule
import io.github.null2264.bonsai.core.di.preferenceModule
import io.github.null2264.bonsai.presentation.home.HomeScreen
import io.github.null2264.bonsai.theme.AppTheme
import org.koin.compose.KoinApplication

@Composable
internal fun App() = KoinApplication(application = {
    modules(appModule, preferenceModule)
}) {
    AppTheme {
        Navigator(HomeScreen)
    }
}

package kodama.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import kodama.ui.di.appModule
import kodama.ui.di.preferenceModule
import kodama.ui.presentation.home.HomeScreen
import org.koin.compose.KoinApplication

@Composable
fun App() = KoinApplication(application = {
    modules(appModule, preferenceModule)
}) {
    Navigator(HomeScreen)
}

package kodama.ui.presentation.utils

import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kodama.ui.presentation.home.HomeScreen
import cafe.adriel.voyager.core.screen.Screen as VScreen

internal abstract class Screen : VScreen {
    override val key: ScreenKey = uniqueScreenKey
}

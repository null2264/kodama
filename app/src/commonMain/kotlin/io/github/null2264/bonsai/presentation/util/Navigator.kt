package io.github.null2264.bonsai.presentation.util

import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.screen.Screen as VScreen

abstract class Screen : VScreen {
    override val key: ScreenKey = uniqueScreenKey
}

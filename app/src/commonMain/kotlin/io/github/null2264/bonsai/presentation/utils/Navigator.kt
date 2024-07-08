package io.github.null2264.bonsai.presentation.utils

import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.screen.Screen as VScreen

internal abstract class Screen : VScreen {
    override val key: ScreenKey = uniqueScreenKey
}

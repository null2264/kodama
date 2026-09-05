package kodama.ui.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel

class MainScreenModel : ScreenModel {

    var canScroll by mutableStateOf(true)
        private set
    var isAtTop by mutableStateOf(true)
        private set

    fun updateScrollBehaviour(isAtTop: Boolean, canScroll: Boolean) {
        if (this.isAtTop != isAtTop) this.isAtTop = isAtTop
        if (this.canScroll != canScroll) this.canScroll = canScroll
    }
}

package kodama.ui.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    var canScroll by mutableStateOf(true)
        private set
    var isAtTop by mutableStateOf(true)
        private set

    private val effectChannel = Channel<Effect>()
    val effect = effectChannel.receiveAsFlow()

    private fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }

    fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.DoNothing -> {}
        }
    }

    fun updateScrollBehaviour(isAtTop: Boolean, canScroll: Boolean) {
        if (this.isAtTop != isAtTop) this.isAtTop = isAtTop
        if (this.canScroll != canScroll) this.canScroll = canScroll
    }
}

sealed interface Intent {
    object DoNothing : Intent
}

sealed interface Effect {
    object DoNothing : Effect
}

package kodama.ui.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kodama.ui.presentation.utils.NotAuthenticatedScreen
import kodama.ui.presentation.utils.Screen

internal object EmptyScreen : Screen(), NotAuthenticatedScreen {

    private fun readResolve(): Any = EmptyScreen

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            LoadingIndicator(Modifier.align(Alignment.Center))
        }
    }
}

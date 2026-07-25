package kodama.ui.presentation.recents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kodama.resources.icons.alternate_email

internal object RecentsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(alternate_email)

            return remember {
                TabOptions(
                    index = 1u,
                    title = "Recents",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
    }
}

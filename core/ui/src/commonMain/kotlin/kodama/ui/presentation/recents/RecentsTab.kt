package kodama.ui.presentation.recents

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kodama.resources.icons.schedule

internal object RecentsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(schedule)

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
        Text("Not yet implemented.")
    }
}

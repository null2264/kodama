package kodama.ui.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kodama.resources.icons.account_circle

internal object ProfileTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(account_circle)

            return remember {
                TabOptions(
                    index = 2u,
                    title = "Profile",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
    }
}

package kodama.ui.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.presentation.home.HomeTab
import kodama.ui.presentation.profile.ProfileTab
import kodama.ui.presentation.recents.RecentsTab
import kodama.ui.presentation.utils.Screen

internal class MainScreen : Screen() {

    @Composable
    override fun Content() {
        TabNavigator(HomeTab) {
            KodamaScaffold(
                onNavigationIconClicked = {},
                appBarType = AppBarType.LARGE,
                bottomBar = {
                    NavigationBar {
                        TabNavigationItem(HomeTab)
                        TabNavigationItem(RecentsTab)
                        // TODO: Maybe make the icon the user's profile picture if they have any
                        TabNavigationItem(ProfileTab)
                    }
                }
            ) { contentPadding ->
                Box(modifier = Modifier.padding(contentPadding)) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(tab.options.icon!!, tab.options.title) }
    )
}

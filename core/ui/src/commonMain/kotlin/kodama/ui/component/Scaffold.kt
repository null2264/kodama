package kodama.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kodama.resources.Res
import kodama.resources.back
import kodama.resources.icons.arrow_back
import org.jetbrains.compose.resources.stringResource

@Composable
expect fun KodamaScaffold(
    onNavigationIconClicked: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    scrollBehavior: JayAppBarScrollBehavior? = null,
    fab: @Composable () -> Unit = {},
    navigationIcon: ImageVector = arrow_back,
    navigationIconLabel: String = stringResource(Res.string.back),
    actions: @Composable RowScope.() -> Unit = {},
    appBarType: AppBarType = AppBarType.LARGE,
    snackbarHost: @Composable () -> Unit = {},
    textFieldState: TextFieldState? = null,
    searchResult: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
)

@Composable
internal fun CommonScaffold(
    onNavigationIconClicked: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    scrollBehavior: JayAppBarScrollBehavior? = null,
    fab: @Composable () -> Unit = {},
    navigationIcon: ImageVector = arrow_back,
    navigationIconLabel: String = stringResource(Res.string.back),
    actions: @Composable RowScope.() -> Unit = {},
    appBarType: AppBarType = AppBarType.LARGE,
    snackbarHost: @Composable () -> Unit = {},
    textFieldState: TextFieldState? = null,
    searchResult: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehaviorOrDefault = scrollBehavior ?: enterAlwaysAppBarScrollBehavior()
    val (color, scrolledColor) = getTopAppBarColor(title)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorOrDefault.nestedScrollConnection),
        floatingActionButton = fab,
        topBar = {
            when (appBarType) {
                AppBarType.SMALL -> JayTopAppBar(
                    title = {
                        Text(text = title)
                    },
                    // modifier = Modifier.statusBarsPadding(),
                    colors = topAppBarColors(
                        containerColor = color,
                        scrolledContainerColor = scrolledColor,
                    ),
                    navigationIcon = {
                        ToolTipButton(
                            toolTipLabel = navigationIconLabel,
                            icon = navigationIcon,
                            buttonClicked = onNavigationIconClicked,
                        )
                    },
                    scrollBehavior = scrollBehaviorOrDefault,
                    actions = actions,
                    textFieldState = textFieldState,
                    searchResult = searchResult,
                )
                AppBarType.LARGE -> JayExpandedTopAppBar(
                    title = {
                        Text(text = title)
                    },
                    // modifier = Modifier.statusBarsPadding(),
                    colors = topAppBarColors(
                        containerColor = color,
                        scrolledContainerColor = scrolledColor,
                    ),
                    navigationIcon = {
                        ToolTipButton(
                            toolTipLabel = navigationIconLabel,
                            icon = navigationIcon,
                            buttonClicked = onNavigationIconClicked,
                        )
                    },
                    scrollBehavior = scrollBehaviorOrDefault,
                    actions = actions,
                    textFieldState = textFieldState,
                    searchResult = searchResult,
                )
                AppBarType.NONE -> {}
            }
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
fun getTopAppBarColor(title: String): Pair<Color, Color> {
    return when (title.isEmpty()) {
        true -> Color.Transparent to Color.Transparent
        false -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.primaryContainer
    }
}

enum class AppBarType {
    NONE,
    SMALL,
    LARGE,
}

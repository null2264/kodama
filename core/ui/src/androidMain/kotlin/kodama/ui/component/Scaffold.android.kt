package kodama.ui.component

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun KodamaScaffold(
    onNavigationIconClicked: () -> Unit,
    modifier: Modifier,
    title: String,
    scrollBehavior: JayAppBarScrollBehavior?,
    fab: @Composable () -> Unit,
    navigationIcon: ImageVector,
    navigationIconLabel: String,
    actions: @Composable RowScope.() -> Unit,
    appBarType: AppBarType,
    snackbarHost: @Composable () -> Unit,
    textFieldState: TextFieldState?,
    searchResult: @Composable (ColumnScope.() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit,
) {
    val view = LocalView.current
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > .5

    SideEffect {
        val activity = view.context as? Activity
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
                activity.window.statusBarColor = Color.Transparent.toArgb()
            WindowInsetsControllerCompat(activity.window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }

    CommonScaffold(
        onNavigationIconClicked = onNavigationIconClicked,
        modifier = modifier,
        title = title,
        scrollBehavior = scrollBehavior,
        fab = fab,
        navigationIcon = navigationIcon,
        navigationIconLabel = navigationIconLabel,
        actions = actions,
        appBarType = appBarType,
        snackbarHost = snackbarHost,
        textFieldState = textFieldState,
        searchResult = searchResult,
        content = content
    )
}

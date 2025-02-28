package kodama.ui.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.Auth
import kodama.preferences.collectAsState
import kodama.resources.IndieFlower_Regular
import kodama.resources.Res
import kodama.resources.cyclone
import kodama.resources.ic_cyclone
import kodama.resources.ic_rotate_right
import kodama.resources.logout
import kodama.resources.run
import kodama.resources.stop
import kodama.ui.UiPreferences
import kodama.ui.presentation.utils.Screen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

internal object HomeScreen : Screen() {
    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()

        val uiPreferences: UiPreferences = koinInject()
        val auth: Auth = koinInject()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.cyclone),
                fontFamily = FontFamily(Font(Res.font.IndieFlower_Regular)),
                style = MaterialTheme.typography.displayLarge
            )

            var isAnimate by remember { mutableStateOf(false) }
            val transition = rememberInfiniteTransition()
            val rotate by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing)
                )
            )

            Image(
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
                    .run { if (isAnimate) rotate(rotate) else this },
                imageVector = vectorResource(Res.drawable.ic_cyclone),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                contentDescription = null
            )

            ElevatedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .widthIn(min = 200.dp),
                onClick = { isAnimate = !isAnimate },
                content = {
                    Icon(vectorResource(Res.drawable.ic_rotate_right), contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        stringResource(if (isAnimate) Res.string.stop else Res.string.run)
                    )
                }
            )

            val currentTheme by uiPreferences.theme().collectAsState()

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
            ) {
                val themes = UiPreferences.Theme.entries
                themes.forEachIndexed { index, entry ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themes.size,
                        ),
                        onClick = { uiPreferences.theme().set(entry) },
                        selected = currentTheme == entry,
                        label = { Text(stringResource(entry.localizedString)) }
                    )
                }
            }

            TextButton(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
                onClick = { coroutineScope.launch { auth.signOut() } },
            ) {
                Text(stringResource(Res.string.logout))
            }
        }
    }
}

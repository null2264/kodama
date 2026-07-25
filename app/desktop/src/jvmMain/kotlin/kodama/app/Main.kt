package kodama.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kodama.core.di.initKoin
import kodama.ui.App
import kodama.ui.di.uiModule
import kodama.ui.theme.KodamaTheme
import org.koin.compose.koinInject

private val Teal = Color(0xFF00687A)

fun main() {
    initKoin(
        additionalDeclaration = {
            modules(uiModule)
        },
    )
    application {
        val mainVisible = remember { mutableStateOf(false) }
        val auth: Auth = koinInject()

        LaunchedEffect(Unit) {
            auth.sessionStatus.collect { status ->
                if (status !is SessionStatus.Initializing) {
                    mainVisible.value = true
                }
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            visible = mainVisible.value,
            title = "Kodama",
        ) {
            KodamaTheme { _ ->
                App()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            visible = !mainVisible.value,
            state = WindowState(width = 400.dp, height = 300.dp),
            title = "Kodama",
            resizable = false,
            undecorated = true,
            alwaysOnTop = true,
            position = WindowPosition(Alignment.Center),
        ) {
            val textMeasurer = rememberTextMeasurer()

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Teal)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f - 20.dp.toPx()
                val outerRadius = 50.dp.toPx()
                val innerRadius = 42.5.dp.toPx()
                val arrowLength = 10.dp.toPx()
                val arrowBaseWidth = 2.5.dp.toPx()

                drawCircle(Color.White, outerRadius, Offset(centerX, centerY))
                drawCircle(Teal, innerRadius, Offset(centerX, centerY))

                val directions = listOf(
                    Offset(0f, -1f),
                    Offset(0f, 1f),
                    Offset(-1f, 0f),
                    Offset(1f, 0f),
                )
                for (dir in directions) {
                    drawCompassArrow(
                        centerX = centerX,
                        centerY = centerY,
                        dirX = dir.x,
                        dirY = dir.y,
                        tipDistance = outerRadius,
                        baseDistance = outerRadius - arrowLength,
                        perpDistance = arrowBaseWidth,
                    )
                }

                val textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
                val textLayout = textMeasurer.measure("Kodama", textStyle)
                drawText(
                    textLayout,
                    topLeft = Offset(
                        centerX - textLayout.size.width / 2f,
                        centerY + outerRadius + 20.dp.toPx(),
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawCompassArrow(
    centerX: Float,
    centerY: Float,
    dirX: Float,
    dirY: Float,
    tipDistance: Float,
    baseDistance: Float,
    perpDistance: Float,
) {
    val tipX = centerX + dirX * tipDistance
    val tipY = centerY + dirY * tipDistance
    val baseCenterX = centerX + dirX * baseDistance
    val baseCenterY = centerY + dirY * baseDistance
    val perpX = -dirY
    val perpY = dirX

    drawLine(
        Color.White,
        Offset(tipX, tipY),
        Offset(baseCenterX + perpX * perpDistance, baseCenterY + perpY * perpDistance),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        Color.White,
        Offset(tipX, tipY),
        Offset(baseCenterX - perpX * perpDistance, baseCenterY - perpY * perpDistance),
        strokeWidth = 2.dp.toPx(),
    )
}

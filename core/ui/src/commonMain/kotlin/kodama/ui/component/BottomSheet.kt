package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class SheetPosition { Collapsed, HalfExpanded, Expanded }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodamaBottomSheet(
    modifier: Modifier = Modifier,
    peekHeightPx: Float = 300f,
    sheetContent: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutHeight = with(LocalDensity.current) { maxHeight.toPx() }

        val state = remember {
            AnchoredDraggableState(
                initialValue = SheetPosition.Collapsed,
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(maxHeight)
                .onSizeChanged { size ->
                    val sheetHeight = size.height.toFloat()
                    state.updateAnchors(
                        DraggableAnchors {
                            SheetPosition.Collapsed at (layoutHeight - peekHeightPx)
                            SheetPosition.HalfExpanded at (layoutHeight - (sheetHeight * 0.6f))
                            SheetPosition.Expanded at (layoutHeight - sheetHeight)
                        }
                    )
                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = state.requireOffset().roundToInt()
                    )
                }
                .anchoredDraggable(state, Orientation.Vertical)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TooltipBox(
                        modifier = modifier,
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip { Text("Dingus") }
                        },
                        state = rememberTooltipState(),
                        content = { BottomSheetDefaults.DragHandle() },
                    )
                }
                sheetContent()
            }
        }
    }
}

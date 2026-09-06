package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class SheetPosition { Collapsed, HalfExpanded, Expanded }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodamaBottomSheet(
    modifier: Modifier = Modifier,
    peekHeightPx: Float = 300f,
    dragHandleToolTipString: String = "Bottom Sheet",
    sheetContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    BoxWithConstraints(
        modifier = modifier
            .layout { measurable, constraints ->
                val calculatedHeight = (constraints.maxHeight + navBarHeightPx).toInt()
                val placeable = measurable.measure(
                    constraints.copy(
                        minHeight = calculatedHeight,
                        maxHeight = calculatedHeight,
                    )
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
    ) {

        val layoutHeight = with(density) { maxHeight.toPx() } + navBarHeightPx

        val state = remember {
            AnchoredDraggableState(
                initialValue = SheetPosition.Collapsed,
            )
        }
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(state)

        val nestedScrollConnection = remember(state) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    // Lift the sheet up first before allowing LazyColumn to scroll its items
                    return if (delta < 0 && state.currentValue != SheetPosition.Expanded) {
                        val consumed = state.dispatchRawDelta(delta)
                        Offset(0f, consumed)
                    } else {
                        Offset.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    return if (delta > 0) {
                        // Drag the sheet down when list hits top boundary
                        val consumedDelta = state.dispatchRawDelta(delta)
                        Offset(0f, consumedDelta)
                    } else {
                        // --- THE CRITICAL FIX ---
                        // Forcefully consume any leftover scrolling when moving UP.
                        // This blocks the gesture from escaping to your Scaffold/App Bar.
                        available
                    }
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    state.anchoredDrag {
                        // The ScrollScope's lifecycle is tied to the AnchoredDragScope we receive
                        //  from anchoredDrag. It is used to bridge AnchoredDraggable and
                        //  FlingBehavior.
                        val scrollFlingScope =
                            object : ScrollScope {
                                override fun scrollBy(pixels: Float): Float {
                                    dragTo(state.offset + pixels)
                                    return pixels
                                }
                            }
                        // Perform a fling with the fling behavior and scroll scope
                        with(flingBehavior) { scrollFlingScope.performFling(available.y) }
                    }
                    return available
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight + with(density) { navBarHeightPx.toDp() })
                .onSizeChanged { size ->
                    val sheetHeight = size.height.toFloat()
                    state.updateAnchors(
                        DraggableAnchors {
                            SheetPosition.Collapsed at (layoutHeight - peekHeightPx - (navBarHeightPx * 0.6f))
                            SheetPosition.HalfExpanded at (layoutHeight - (sheetHeight * 0.6f))
                            SheetPosition.Expanded at (layoutHeight - sheetHeight - (navBarHeightPx * 0.6f))
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
                .nestedScroll(nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip { Text(dragHandleToolTipString) }
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

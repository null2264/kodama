package kodama.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kodama.resources.Res
import kodama.resources.delete_bonsai
import kodama.resources.finalize_bonsai
import kodama.resources.icons.chevron
import kodama.resources.icons.delete
import kodama.resources.icons.edit
import kodama.ui.presentation.contest.slop.FinalizeEntryScreen
import org.jetbrains.compose.resources.stringResource

/**
 * SplitButton layout with Dropdown menu already included
 */
@Composable
fun DropdownSplitButton(
    modifier: Modifier = Modifier,
    leadingButton: @Composable () -> Unit,
    dropdownItems: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = leadingButton,
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = dropdownExpanded,
                    onCheckedChange = { dropdownExpanded = true },
                ) {
                    val rotation: Float by animateFloatAsState(
                        targetValue = if (dropdownExpanded) 180f else 0f,
                        label = "Trailing Icon Rotation"
                    )
                    Icon(
                        chevron,
                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer { rotationZ = rotation },
                        contentDescription = "More action"
                    )
                }
            }
        )
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = { dropdownItems { dropdownExpanded = false } },
        )
    }
}

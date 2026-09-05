package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kodama.resources.icons.account_circle

@Composable
fun Chip(
    text: String,
    icon: ImageVector? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(backgroundColor),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.padding(start = 6.dp).size(20.dp),
                    imageVector = icon,
                    contentDescription = "Chip icon",
                )
            }
            Text(
                modifier = Modifier.padding(
                    start = if (icon != null) 6.dp else 10.dp,
                    end = 10.dp,
                ),
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Preview
@Composable
private fun ChipPreview() {
    Chip("Dingus", account_circle)
}

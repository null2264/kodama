package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kodama.resources.icons.account_circle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Chip(
    text: String,
    icon: ImageVector? = null,
    fallbackShape: Shape? = null,
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
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else if (fallbackShape != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 2.dp)
                        .padding(vertical = 2.dp)
                        .size(16.dp)
                        .border(1.3.dp, MaterialTheme.colorScheme.onPrimaryContainer, fallbackShape),
                )
            }
            Text(
                modifier = Modifier.padding(
                    start = if (icon != null || fallbackShape != null) 6.dp else 10.dp,
                    end = 10.dp,
                ),
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ChipPreview() {
    Column {
        Chip("Dingus", account_circle)
        Chip("Dingus", fallbackShape = MaterialShapes.Clover4Leaf.toShape())
    }
}

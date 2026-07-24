package kodama.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun BonsaiPict(
    pictPath: String?,
    modifier: Modifier = Modifier,
    supabaseUrl: String,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (pictPath != null) {
        AsyncImage(
            model = "$supabaseUrl/storage/v1/object/public/kodama-images/$pictPath",
            contentDescription = "Bonsai picture",
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No Image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ContestBanner(
    bannerPath: String?,
    modifier: Modifier = Modifier,
    supabaseUrl: String,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (bannerPath != null) {
        AsyncImage(
            model = "$supabaseUrl/storage/v1/object/public/kodama-images/$bannerPath",
            contentDescription = "Contest banner",
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = contentScale,
        )
    }
}

@Composable
fun ImageUploader(
    currentPath: String?,
    onPick: () -> Unit,
    supabaseUrl: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        BonsaiPict(
            pictPath = currentPath,
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            supabaseUrl = supabaseUrl,
        )
        Button(
            onClick = onPick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
            Text("Pick Image")
        }
    }
}
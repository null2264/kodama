package kodama.ui.component

import androidx.compose.runtime.Composable

expect @Composable fun rememberImageFilePicker(): ImageFilePicker

class ImageFilePicker(
    val pick: suspend () -> ByteArray?,
)

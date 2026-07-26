package kodama.ui.component

import androidx.compose.runtime.Composable

expect @Composable fun rememberImageFilePicker(): ImageFilePicker

class ImageFilePicker(
    val pick: suspend () -> ByteArray?,
)

data class DocumentFilePickerResult(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

expect @Composable fun rememberDocumentFilePicker(): DocumentFilePicker

class DocumentFilePicker(
    val pick: suspend () -> DocumentFilePickerResult?,
)

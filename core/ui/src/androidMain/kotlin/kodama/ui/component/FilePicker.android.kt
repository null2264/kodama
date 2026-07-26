package kodama.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberImageFilePicker(): ImageFilePicker {
    val context = LocalContext.current
    var continuation: ((ByteArray?) -> Unit)? = null

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val bytes = uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
        }
        continuation?.invoke(bytes)
    }

    return remember {
        ImageFilePicker(
            pick = {
                suspendCancellableCoroutine { cont ->
                    continuation = { bytes ->
                        cont.resume(bytes)
                    }
                    launcher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            }
        )
    }
}

@Composable
actual fun rememberDocumentFilePicker(): DocumentFilePicker {
    val context = LocalContext.current
    var continuation: ((DocumentFilePickerResult?) -> Unit)? = null

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val result = uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                c.getString(nameIndex)
            } ?: "document"
            bytes?.let { b -> DocumentFilePickerResult(b, fileName, mimeType) }
        }
        continuation?.invoke(result)
    }

    return remember {
        DocumentFilePicker(
            pick = {
                suspendCancellableCoroutine { cont ->
                    continuation = { result ->
                        cont.resume(result)
                    }
                    launcher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                }
            }
        )
    }
}

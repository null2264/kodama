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

package kodama.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javafx.application.Platform
import javafx.stage.FileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Composable
actual fun rememberImageFilePicker(): ImageFilePicker {
    return remember {
        ImageFilePicker(
            pick = {
                suspendCancellableCoroutine { cont ->
                    Platform.startup {
                        val fileChooser = FileChooser().apply {
                            title = "Select Image"
                            extensionFilters.addAll(
                                FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
                            )
                        }

                        val window = null
                        val file = fileChooser.showOpenDialog(window)

                        cont.resume(file?.readBytes())
                    }
                }
            }
        )
    }
}

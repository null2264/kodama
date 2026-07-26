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

@Composable
actual fun rememberDocumentFilePicker(): DocumentFilePicker {
    return remember {
        DocumentFilePicker(
            pick = {
                suspendCancellableCoroutine { cont ->
                    Platform.startup {
                        val fileChooser = FileChooser().apply {
                            title = "Pilih Bukti Bayar"
                            extensionFilters.addAll(
                                FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.doc", "*.docx")
                            )
                        }

                        val window = null
                        val file = fileChooser.showOpenDialog(window)

                        val result = file?.let { f ->
                            val ext = f.extension.lowercase()
                            val contentType = when (ext) {
                                "pdf" -> "application/pdf"
                                "png" -> "image/png"
                                "jpg", "jpeg" -> "image/jpeg"
                                "webp" -> "image/webp"
                                "doc" -> "application/msword"
                                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                else -> "application/octet-stream"
                            }
                            DocumentFilePickerResult(f.readBytes(), f.name, contentType)
                        }
                        cont.resume(result)
                    }
                }
            }
        )
    }
}

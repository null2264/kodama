package kodama.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class AlertDialogBuilder {
    var title: String = ""
    var titleRes: StringResource? = null

    var text: String? = null
    var textRes: StringResource? = null

    var confirmText: String? = "OK"
    var confirmTextRes: StringResource? = null

    var cancelText: String? = null
    var cancelTextRes: StringResource? = null

    var shouldContinue: Boolean = true

    var onConfirm: () -> Unit = {}
    var onCancel: () -> Unit = {}

    var onDismissAction: (() -> Unit)? = null
    private val resolvedOnDismiss: () -> Unit
        get() = onDismissAction ?: onCancel

    @Suppress("ComposableNaming")
    @Composable
    fun build() {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                val resolvedTitle = titleRes?.let { stringResource(it) } ?: title
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = if (textRes == null && text == null) null else {
                {
                    val resolvedText = textRes?.let { stringResource(it) } ?: text.orEmpty()
                    Text(
                        text = resolvedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onDismissRequest = {
                resolvedOnDismiss()
            },
            confirmButton = {
                val resolvedConfirm = confirmTextRes?.let { stringResource(it) } ?: confirmText ?: "OK"
                TextButton(
                    onClick = {
                        onConfirm()
                    }
                ) {
                    Text(
                        text = resolvedConfirm,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = run {
                val hasCancelText = cancelText != null || cancelTextRes != null
                if (!hasCancelText) null else {
                    @Composable {
                        val resolvedCancel = cancelTextRes?.let { stringResource(it) } ?: cancelText.orEmpty()
                        TextButton(
                            onClick = {
                                onCancel()
                            }
                        ) {
                            Text(
                                text = resolvedCancel,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

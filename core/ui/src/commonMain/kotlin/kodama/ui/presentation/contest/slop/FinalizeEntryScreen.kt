package kodama.ui.presentation.contest.slop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.LoadingButton
import kodama.ui.component.rememberDocumentFilePicker
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

internal class FinalizeEntryScreen(
    private val contestId: String,
    private val bonsaiId: String,
) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<FinalizeEntryScreenModel> {
            parametersOf(contestId, bonsaiId)
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val coroutineScope = rememberCoroutineScope()
        val documentPicker = rememberDocumentFilePicker()

        var alertDialogMessage by remember { mutableStateOf<String?>(null) }
        var showConfirmDialog by remember { mutableStateOf(false) }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = "Finalisasi Pendaftaran",
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = "Bukti Pembayaran",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = "*wajib",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LoadingButton(
                        onClick = {
                            coroutineScope.launch {
                                val result = documentPicker.pick()
                                if (result != null) {
                                    screenModel.onReceiptPicked(result.bytes, result.fileName, result.contentType)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = false,
                        enabled = true,
                    ) {
                        Text(if (state.receiptFileName != null) "Ganti Bukti Bayar" else "Unggah Bukti Bayar")
                    }
                    state.receiptFileName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                LoadingButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    isLoading = state.isFinalizing,
                    enabled = state.receiptFileName != null && !state.isFinalizing,
                ) {
                    Text("Finalisasi")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showConfirmDialog) {
            AlertDialogBuilder().apply {
                title = "Apakah Anda yakin?"
                text = "Finalisasi pendaftaran akan mengubah data menjadi read-only dan tidak dapat diedit lagi."
                confirmText = "Ya, Finalisasi"
                cancelText = "Batal"
                onConfirm = {
                    showConfirmDialog = false
                    screenModel.finalizeEntry(
                        onError = { alertDialogMessage = it },
                        onSuccess = { navigator?.pop() },
                    )
                }
                onCancel = { showConfirmDialog = false }
            }.build()
        }

        alertDialogMessage?.let { message ->
            AlertDialogBuilder().apply {
                title = "Terjadi kesalahan!"
                text = message
                onConfirm = { alertDialogMessage = null }
                onCancel = { alertDialogMessage = null }
            }.build()
        }
    }
}

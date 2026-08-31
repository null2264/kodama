package kodama.ui.presentation.contest

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.github.goquati.qr.QrCode
import kodama.resources.Res
import kodama.resources.bonsai_detail
import kodama.resources.finalize_bonsai
import kodama.resources.qr_title
import kodama.resources.show_qr
import kodama.ui.component.AppBarType
import kodama.ui.component.BonsaiPict
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal class BonsaiDetailScreen(
    private val contestId: String,
    private val bonsaiId: String,
) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<BonsaiDetailScreenModel> {
            org.koin.core.parameter.parametersOf(contestId, bonsaiId)
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val supabaseClient: io.github.jan.supabase.SupabaseClient = koinInject()
        val supabaseUrl = supabaseClient.config.supabaseUrl

        var showQrDialog by remember { mutableStateOf(false) }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = stringResource(Res.string.bonsai_detail),
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                state.bonsai != null -> {
                    val bonsai = state.bonsai!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        BonsaiPict(
                            pictPath = bonsai.pict_path,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            supabaseUrl = supabaseUrl,
                            contentScale = ContentScale.Crop,
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = bonsai.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            state.bonsaiClass?.let { cls ->
                                Text(
                                    text = cls.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        when {
                            state.isOwner && bonsai.state == "draft" -> {
                                LoadingButton(
                                    onClick = {
                                        navigator?.push(FinalizeEntryScreen(contestId, bonsaiId))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isLoading = false,
                                    enabled = true,
                                ) {
                                    Text(stringResource(Res.string.finalize_bonsai))
                                }
                            }
                            state.isOwner && bonsai.state != "draft" -> {
                                LoadingButton(
                                    onClick = { showQrDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    isLoading = false,
                                    enabled = true,
                                ) {
                                    Text(stringResource(Res.string.show_qr))
                                }
                            }
                            state.isJudge -> {
                                LoadingButton(
                                    onClick = {
                                        navigator?.push(RatingScreen(contestId, bonsaiId))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isLoading = false,
                                    enabled = true,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                ) {
                                    Text("Rate")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (showQrDialog) {
            val qrBitmap = remember(state.qrUri) {
                val qr = QrCode.encodeText(state.qrUri, QrCode.Ecc.MEDIUM)
                val moduleSize = 10
                val padding = 4
                val size = qr.size
                val totalSize = (size + padding * 2) * moduleSize
                val bgPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.White
                }
                val fgPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.Black
                }
                ImageBitmap(totalSize, totalSize).apply {
                    androidx.compose.ui.graphics.Canvas(this).apply {
                        drawRect(
                            androidx.compose.ui.geometry.Rect(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
                            bgPaint,
                        )
                        for (y in 0 until size) {
                            for (x in 0 until size) {
                                if (qr[x, y]) {
                                    val left = ((padding + x) * moduleSize).toFloat()
                                    val top = ((padding + y) * moduleSize).toFloat()
                                    drawRect(
                                        androidx.compose.ui.geometry.Rect(left, top, left + moduleSize, top + moduleSize),
                                        fgPaint,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showQrDialog = false },
                title = {
                    Text(
                        text = stringResource(Res.string.qr_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = stringResource(Res.string.qr_title),
                            modifier = Modifier.size(250.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.bonsai?.name ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrDialog = false }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

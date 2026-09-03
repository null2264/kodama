package kodama.ui.presentation.contest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import kodama.resources.Res
import kodama.resources.icons.alternate_email
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.KodamaTextField
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.stringResource

internal class RatingScreen(
    private val contestId: String,
    private val bonsaiId: String,
) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<RatingScreenModel> {
            org.koin.core.parameter.parametersOf(contestId, bonsaiId)
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current

        var alertDialogMessage by remember { mutableStateOf<String?>(null) }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = "Rating: Entry #${bonsaiId.takeLast(4)}",
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
                                text = state.bonsai?.name ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Bonsai Type",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Regional") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Miniature") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                )
                            }
                        }

                        Text(
                            text = "Rate the entry (Score 1 to 100)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        RatingSlider(
                            label = "Penampilan",
                            value = state.scores["penampilan"] ?: 50,
                            onValueChange = { screenModel.onScoreChanged("penampilan", it) },
                        )

                        RatingSlider(
                            label = "Gerak Dasar",
                            value = state.scores["gerak_dasar"] ?: 50,
                            onValueChange = { screenModel.onScoreChanged("gerak_dasar", it) },
                        )

                        RatingSlider(
                            label = "Keserasian",
                            value = state.scores["keserasian"] ?: 50,
                            onValueChange = { screenModel.onScoreChanged("keserasian", it) },
                        )

                        RatingSlider(
                            label = "Kematangan",
                            value = state.scores["kematangan"] ?: 50,
                            onValueChange = { screenModel.onScoreChanged("kematangan", it) },
                        )

                        KodamaTextField(
                            value = state.comments,
                            onValueChange = { screenModel.onCommentsChanged(it) },
                            label = "Judge Notes (Optional)",
                            placeholder = "Hello World",
                            singleLine = false,
                            icon = { Icon(alternate_email, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        )

                        Text(
                            text = "Current Total Score: ${state.scores.values.sum()}/400",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        LoadingButton(
                            onClick = {
                                screenModel.submitReview(
                                    onError = { alertDialogMessage = it },
                                    onSuccess = { navigator?.pop() },
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            isLoading = state.isSubmitting,
                            enabled = !state.isSubmitting,
                        ) {
                            Text("Submit Score")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            alertDialogMessage?.let { message ->
                kodama.ui.component.AlertDialogBuilder().apply {
                    title = "Terjadi kesalahan!"
                    text = message
                    onConfirm = { alertDialogMessage = null }
                    onCancel = { alertDialogMessage = null }
                }.build()
            }
        }
    }
}

@Composable
private fun RatingSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

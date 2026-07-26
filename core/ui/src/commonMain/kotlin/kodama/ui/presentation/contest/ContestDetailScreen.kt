package kodama.ui.presentation.contest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import kodama.core.util.isAdmin
import kodama.resources.Res
import kodama.resources.contest_classes_label
import kodama.resources.contest_created_success
import kodama.resources.contest_detail_title
import kodama.resources.icons.edit
import kodama.core.util.kodamaRole
import kodama.ui.component.AppBarType
import kodama.ui.component.ContestBanner
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.ToolTipButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal class ContestDetailScreen(
    private val contestId: String,
    private val showCreatedSnackbar: Boolean = false,
) : Screen() {

    @OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<ContestDetailScreenModel> {
            org.koin.core.parameter.parametersOf(contestId)
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val snackbarHostState = remember { SnackbarHostState() }
        val successMessage = stringResource(Res.string.contest_created_success)
        val supabaseClient: SupabaseClient = koinInject()
        val supabaseUrl = supabaseClient.config.supabaseUrl
        val auth: Auth = koinInject()
        val isAdmin = auth.currentUserOrNull().isAdmin

        LaunchedEffect(showCreatedSnackbar) {
            if (showCreatedSnackbar) {
                snackbarHostState.showSnackbar(successMessage)
            }
        }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = state.contest?.name ?: stringResource(Res.string.contest_detail_title),
            appBarType = AppBarType.SMALL,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            actions = {
                if (isAdmin && state.contest?.state == "draft") {
                    ToolTipButton(
                        toolTipLabel = "Edit",
                        icon = edit,
                        buttonClicked = { navigator?.parent?.push(EditContestScreen(contestId)) },
                    )
                }
            },
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
                state.contest != null -> {
                    val contest = state.contest!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        contest.banner_path?.let { bannerPath ->
                            ContestBanner(
                                bannerPath = bannerPath,
                                modifier = Modifier.fillMaxWidth(),
                                supabaseUrl = supabaseUrl,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = contest.name,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = contest.state.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        contest.description?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (state.classes.isNotEmpty()) {
                            Column {
                                Text(
                                    text = stringResource(Res.string.contest_classes_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.classes.forEach { bonsaiClass ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(bonsaiClass.name) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Lomba tidak ditemukan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

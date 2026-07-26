package kodama.ui.presentation.contest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import kodama.core.data.BonsaiWithMetadata
import kodama.core.util.isAdmin
import kodama.resources.Res
import kodama.resources.bonsai_list
import kodama.resources.bonsai_pending_verification
import kodama.resources.bonsai_voting_progress
import kodama.resources.contest_classes_label
import kodama.resources.contest_created_success
import kodama.resources.contest_detail_title
import kodama.resources.finalize_contest
import kodama.resources.finalize_contest_confirm_text
import kodama.resources.finalize_contest_confirm_title
import kodama.resources.icons.edit
import kodama.resources.judges_voted_format
import kodama.resources.not_voted
import kodama.resources.verify_bonsai
import kodama.resources.voted
import kodama.resources.voting_progress_format
import kodama.ui.component.AppBarType
import kodama.ui.component.ContestBanner
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.LoadingButton
import kodama.ui.component.ToolTipButton
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import kotlinx.coroutines.launch
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
        val currentUser = auth.currentUserOrNull()
        val isAdmin = currentUser.isAdmin
        val currentUserId = currentUser?.id
        var showFinalizeDialog by remember { mutableStateOf(false) }
        var showBottomSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()

        val canShowSheet = state.contest?.state == "accepting" || state.contest?.state == "reviewing"

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

                        if (isAdmin && contest.state == "draft") {
                            LoadingButton(
                                onClick = { showFinalizeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = state.isFinalizing,
                                enabled = !state.isFinalizing,
                            ) {
                                Text(stringResource(Res.string.finalize_contest))
                            }
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

                        if (canShowSheet) {
                            AssistChip(
                                onClick = {
                                    coroutineScope.launch {
                                        sheetState.show()
                                        showBottomSheet = true
                                    }
                                },
                                label = { Text(stringResource(Res.string.bonsai_list)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
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

            if (showFinalizeDialog) {
                kodama.ui.component.AlertDialogBuilder().apply {
                    titleRes = Res.string.finalize_contest_confirm_title
                    textRes = Res.string.finalize_contest_confirm_text
                    confirmText = "Ya, Buka"
                    cancelText = "Batal"
                    onConfirm = {
                        showFinalizeDialog = false
                        screenModel.finalizeContest(
                            onError = { },
                            onSuccess = { },
                        )
                    }
                    onCancel = { showFinalizeDialog = false }
                }.build()
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                ) {
                    val contestState = state.contest?.state

                    when {
                        state.isSheetLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        contestState == "accepting" && isAdmin -> {
                            AdminAcceptingSheet(
                                bonsaiList = state.bonsaiList.filter { it.state == "waiting_verify" },
                                onVerify = { bonsaiId ->
                                    screenModel.verifyBonsai(bonsaiId)
                                },
                            )
                        }
                        contestState == "reviewing" && isAdmin -> {
                            AdminReviewingSheet(
                                bonsaiList = state.bonsaiList.filter { it.state == "verified" },
                                reviews = state.reviews,
                                contestUsers = state.contestUsers,
                            )
                        }
                        contestState == "reviewing" -> {
                            JudgeReviewingSheet(
                                bonsaiList = state.bonsaiList.filter { it.state == "verified" },
                                reviews = state.reviews,
                                currentUserId = currentUserId,
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tidak ada data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAcceptingSheet(
    bonsaiList: List<BonsaiWithMetadata>,
    onVerify: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(Res.string.bonsai_pending_verification),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (bonsaiList.isEmpty()) {
            Text(
                text = "Tidak ada bonsai menunggu verifikasi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bonsaiList, key = { it.id }) { bonsai ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bonsai.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            TextButton(
                                onClick = { onVerify(bonsai.id) },
                            ) {
                                Text(stringResource(Res.string.verify_bonsai))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReviewingSheet(
    bonsaiList: List<BonsaiWithMetadata>,
    reviews: List<kodama.core.data.Review>,
    contestUsers: List<kodama.core.data.ContestUser>,
) {
    val totalJudges = contestUsers.count { it.role == "judge" || it.role == "head_judge" }
    val totalReviews = bonsaiList.size * totalJudges

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(Res.string.bonsai_voting_progress),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (bonsaiList.isEmpty()) {
            Text(
                text = "Tidak ada bonsai untuk dinilai",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            val completedReviews = reviews.size
            val progress = if (totalReviews > 0) {
                completedReviews.toFloat() / totalReviews
            } else {
                0f
            }

            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.voting_progress_format, completedReviews, totalReviews),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bonsaiList, key = { it.id }) { bonsai ->
                    val bonsaiReviews = reviews.filter { it.bonsai_id == bonsai.id }
                    val judgesForClass = contestUsers.count {
                        it.role == "judge" && it.contest_class_id == bonsai.contest_class_id
                    } + contestUsers.count { it.role == "head_judge" }
                    val votedCount = bonsaiReviews.size

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Text(
                                text = bonsai.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.judges_voted_format, votedCount, judgesForClass),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JudgeReviewingSheet(
    bonsaiList: List<BonsaiWithMetadata>,
    reviews: List<kodama.core.data.Review>,
    currentUserId: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(Res.string.bonsai_list),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (bonsaiList.isEmpty()) {
            Text(
                text = "Tidak ada bonsai untuk dinilai",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bonsaiList, key = { it.id }) { bonsai ->
                    val hasVoted = currentUserId != null &&
                        reviews.any { it.bonsai_id == bonsai.id && it.judge_id == currentUserId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = bonsai.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = if (hasVoted) {
                                    stringResource(Res.string.voted)
                                } else {
                                    stringResource(Res.string.not_voted)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasVoted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

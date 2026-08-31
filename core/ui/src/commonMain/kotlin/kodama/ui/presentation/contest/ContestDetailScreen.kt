package kodama.ui.presentation.contest

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.Review
import kodama.core.util.isAdmin
import kodama.resources.Res
import kodama.resources.bonsai_list
import kodama.resources.bonsai_pending_verification
import kodama.resources.bonsai_state_draft
import kodama.resources.bonsai_state_verified
import kodama.resources.bonsai_state_waiting_verify
import kodama.resources.bonsai_voting_progress
import kodama.resources.contest_classes_label
import kodama.resources.contest_created_success
import kodama.resources.contest_detail_title
import kodama.resources.delete_bonsai
import kodama.resources.delete_bonsai_confirm_text
import kodama.resources.delete_bonsai_confirm_title
import kodama.resources.finalize_bonsai
import kodama.resources.finalize_bonsai_confirm_text
import kodama.resources.finalize_bonsai_confirm_title
import kodama.resources.finalize_contest
import kodama.resources.finalize_contest_confirm_text
import kodama.resources.finalize_contest_confirm_title
import kodama.resources.icons.edit
import kodama.resources.judges_voted_format
import kodama.resources.my_bonsai
import kodama.resources.not_voted
import kodama.resources.register_bonsai
import kodama.resources.verify_bonsai
import kodama.resources.voted
import kodama.resources.voting_progress_format
import kodama.resources.view_payment_proof
import kodama.resources.view_results
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
        var showStateTransitionDialog by remember { mutableStateOf(false) }
        var pendingTransitionState by remember { mutableStateOf<String?>(null) }
        var showFinishDialog by remember { mutableStateOf(false) }
        var showForceCloseDialog by remember { mutableStateOf(false) }
        var showBottomSheet by remember { mutableStateOf(false) }
        var bonsaiToFinalize by remember { mutableStateOf<Bonsai?>(null) }
        var bonsaiToDelete by remember { mutableStateOf<Bonsai?>(null) }
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()
        val isFinishedOrEnded = state.contest?.state == "finished" || state.contest?.state == "ended"
        val isReadOnly = isFinishedOrEnded

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
                        buttonClicked = { navigator?.push(EditContestScreen(contestId)) },
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
                                isLoading = state.isUpdatingState,
                                enabled = !state.isUpdatingState,
                            ) {
                                Text(stringResource(Res.string.finalize_contest))
                            }
                        }

                        if (isAdmin && contest.state == "draft") {
                            AssistChip(
                                onClick = { navigator?.push(AssignJudgesScreen(contestId)) },
                                label = { Text("Assign Judges") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                ),
                            )
                        }

                        if (isAdmin && contest.state == "accepting") {
                            LoadingButton(
                                onClick = {
                                    pendingTransitionState = "closed"
                                    showStateTransitionDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = state.isUpdatingState,
                                enabled = !state.isUpdatingState,
                            ) {
                                Text("Close Registration")
                            }
                        }

                        if (isAdmin && contest.state == "closed") {
                            LoadingButton(
                                onClick = {
                                    pendingTransitionState = "reviewing"
                                    showStateTransitionDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = state.isUpdatingState,
                                enabled = !state.isUpdatingState,
                            ) {
                                Text("Open Reviewing")
                            }
                        }

                        if (isAdmin && contest.state == "reviewing") {
                            LoadingButton(
                                onClick = { showFinishDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = state.isUpdatingState,
                                enabled = !state.isUpdatingState,
                            ) {
                                Text("Finish Contest")
                            }
                        }

                        if (isAdmin && contest.state == "finished") {
                            LoadingButton(
                                onClick = {
                                    pendingTransitionState = "ended"
                                    showStateTransitionDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = state.isUpdatingState,
                                enabled = !state.isUpdatingState,
                            ) {
                                Text("End Contest")
                            }
                        }

                        if (isFinishedOrEnded) {
                            AssistChip(
                                onClick = { navigator?.push(ResultsScreen(contestId)) },
                                label = { Text(stringResource(Res.string.view_results)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
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

                        if (!isAdmin && contest.state == "accepting") {
                            Column {
                                Text(
                                    text = stringResource(Res.string.my_bonsai),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )

                                state.myBonsai.forEach { bonsai ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable {
                                                navigator?.push(BonsaiDetailScreen(contestId, bonsai.id))
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = bonsai.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    text = when (bonsai.state) {
                                                        "draft" -> stringResource(Res.string.bonsai_state_draft)
                                                        "waiting_verify" -> stringResource(Res.string.bonsai_state_waiting_verify)
                                                        "verified" -> stringResource(Res.string.bonsai_state_verified)
                                                        else -> bonsai.state
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (bonsai.state) {
                                                        "draft" -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        "waiting_verify" -> MaterialTheme.colorScheme.tertiary
                                                        "verified" -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                )
                                            }

                                            if (bonsai.state == "draft" && !isReadOnly) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    LoadingButton(
                                                        onClick = {
                                                            navigator?.push(FinalizeEntryScreen(contestId, bonsai.id))
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        isLoading = false,
                                                        enabled = true,
                                                    ) {
                                                        Text(stringResource(Res.string.finalize_bonsai))
                                                    }
                                                    TextButton(
                                                        onClick = { bonsaiToDelete = bonsai },
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.delete_bonsai),
                                                            color = MaterialTheme.colorScheme.error,
                                                        )
                                                    }
                                                }
                                            }

                                            if (contest.state == "finished" || contest.state == "ended") {
                                                val bonsaiReviews = state.reviews.filter { it.bonsai_id == bonsai.id }
                                                if (bonsaiReviews.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Scores:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    bonsaiReviews.forEach { review ->
                                                        Text(
                                                            text = "Total: ${review.total_score}/40",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isReadOnly) {
                                    AssistChip(
                                        onClick = {
                                            navigator?.push(
                                                CreateBonsaiScreen(contestId, state.classes.map { it.id })
                                            )
                                        },
                                        label = { Text(stringResource(Res.string.register_bonsai)) },
                                    )
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

            if (showFinalizeDialog) {
                kodama.ui.component.AlertDialogBuilder().apply {
                    titleRes = Res.string.finalize_contest_confirm_title
                    textRes = Res.string.finalize_contest_confirm_text
                    confirmText = "Ya, Buka"
                    cancelText = "Batal"
                    onConfirm = {
                        showFinalizeDialog = false
                        screenModel.transitionContestState(
                            newState = "accepting",
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            },
                            onSuccess = { },
                        )
                    }
                    onCancel = { showFinalizeDialog = false }
                }.build()
            }

            if (showStateTransitionDialog) {
                kodama.ui.component.AlertDialogBuilder().apply {
                    title = "Konfirmasi"
                    text = "Apakah Anda yakin ingin mengubah state lomba?"
                    confirmText = "Ya"
                    cancelText = "Batal"
                    onConfirm = {
                        showStateTransitionDialog = false
                        pendingTransitionState?.let { newState ->
                            screenModel.transitionContestState(
                                newState = newState,
                                onError = { error ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                },
                                onSuccess = { },
                            )
                            pendingTransitionState = null
                        }
                    }
                    onCancel = {
                        showStateTransitionDialog = false
                        pendingTransitionState = null
                    }
                }.build()
            }

            if (showFinishDialog) {
                kodama.ui.component.AlertDialogBuilder().apply {
                    title = "Finish Contest"
                    text = "Semua review sudah selesai?"
                    confirmText = "Finish"
                    cancelText = "Force Close"
                    onConfirm = {
                        showFinishDialog = false
                        screenModel.finishContest(
                            force = false,
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            },
                            onSuccess = { },
                        )
                    }
                    onCancel = {
                        showFinishDialog = false
                        showForceCloseDialog = true
                    }
                }.build()
            }

            if (showForceCloseDialog) {
                kodama.ui.component.AlertDialogBuilder().apply {
                    title = "Force Close"
                    text = "Force close akan menyelesaikan lomba meskipun semua review belum selesai. Yakin?"
                    confirmText = "Ya, Force Close"
                    cancelText = "Batal"
                    onConfirm = {
                        showForceCloseDialog = false
                        screenModel.finishContest(
                            force = true,
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            },
                            onSuccess = { },
                        )
                    }
                    onCancel = { showForceCloseDialog = false }
                }.build()
            }

            bonsaiToFinalize?.let { bonsai ->
                kodama.ui.component.AlertDialogBuilder().apply {
                    titleRes = Res.string.finalize_bonsai_confirm_title
                    textRes = Res.string.finalize_bonsai_confirm_text
                    confirmText = "Ya, Finalisasi"
                    cancelText = "Batal"
                    onConfirm = {
                        bonsaiToFinalize = null
                        screenModel.finalizeBonsai(bonsai.id)
                    }
                    onCancel = { bonsaiToFinalize = null }
                }.build()
            }

            bonsaiToDelete?.let { bonsai ->
                kodama.ui.component.AlertDialogBuilder().apply {
                    titleRes = Res.string.delete_bonsai_confirm_title
                    textRes = Res.string.delete_bonsai_confirm_text
                    confirmText = "Hapus"
                    cancelText = "Batal"
                    onConfirm = {
                        bonsaiToDelete = null
                        screenModel.deleteBonsai(bonsai.id)
                    }
                    onCancel = { bonsaiToDelete = null }
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
                                supabaseUrl = supabaseUrl,
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
                                contestId = contestId,
                                onRateBonsai = { bonsaiId ->
                                    navigator?.push(RatingScreen(contestId, bonsaiId))
                                },
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
    bonsaiList: List<Bonsai>,
    onVerify: (String) -> Unit,
    supabaseUrl: String,
) {
    val uriHandler = LocalUriHandler.current

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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                            bonsai.payment_proof_path?.let { proofPath ->
                                val proofUrl = "$supabaseUrl/storage/v1/object/public/kodama-images/$proofPath"
                                TextButton(
                                    onClick = {
                                        uriHandler.openUri(proofUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.view_payment_proof),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
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
    bonsaiList: List<Bonsai>,
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
    bonsaiList: List<Bonsai>,
    reviews: List<kodama.core.data.Review>,
    currentUserId: String?,
    contestId: String = "",
    onRateBonsai: (String) -> Unit = {},
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

        val votedCount = if (currentUserId != null) {
            reviews.count { it.judge_id == currentUserId }
        } else {
            0
        }
        val totalBonsai = bonsaiList.size
        val progress = if (totalBonsai > 0) {
            votedCount.toFloat() / totalBonsai
        } else {
            0f
        }

        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$votedCount/$totalBonsai rated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
                            if (hasVoted) {
                                Text(
                                    text = stringResource(Res.string.voted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                TextButton(
                                    onClick = { onRateBonsai(bonsai.id) },
                                ) {
                                    Text("Rate")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

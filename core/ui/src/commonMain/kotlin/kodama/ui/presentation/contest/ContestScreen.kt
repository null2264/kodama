package kodama.ui.presentation.contest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.internal.BackHandler
import coil3.compose.AsyncImage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import kodama.core.data.Bonsai
import kodama.core.data.ImageRepository
import kodama.core.data.model.ContestState
import kodama.core.util.isAdmin
import kodama.core.util.isJudge
import kodama.resources.Res
import kodama.resources.delete_bonsai
import kodama.resources.delete_bonsai_confirm_text
import kodama.resources.delete_bonsai_confirm_title
import kodama.resources.finalize_bonsai
import kodama.resources.finalize_contest
import kodama.resources.finalize_contest_confirm_text
import kodama.resources.finalize_contest_confirm_title
import kodama.resources.icons.account_circle
import kodama.resources.icons.alternate_email
import kodama.resources.icons.chevron
import kodama.resources.icons.delete
import kodama.resources.icons.edit
import kodama.resources.icons.flag
import kodama.resources.icons.rate_review
import kodama.resources.icons.reviews
import kodama.resources.icons.schedule
import kodama.resources.icons.verified
import kodama.resources.judges_voted_format
import kodama.resources.verify_bonsai
import kodama.resources.voted
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.Chip
import kodama.ui.component.DropdownSplitButton
import kodama.ui.component.KodamaBottomSheet
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.LoadingButton
import kodama.ui.component.SheetPosition
import kodama.ui.component.rememberBottomSheetState
import kodama.ui.presentation.bonsai.BonsaiDetailScreen
import kodama.ui.presentation.bonsai.getFlagPotential
import kodama.ui.presentation.contest.slop.AssignJudgesScreen
import kodama.ui.presentation.contest.slop.CreateBonsaiScreen
import kodama.ui.presentation.contest.slop.FinalizeEntryScreen
import kodama.ui.presentation.utils.Screen
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.collections.count
import kotlin.collections.orEmpty
import kotlin.time.Instant

internal class ContestScreen(
    private val contestId: String,
) : Screen() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<ContestViewModel> {
            parametersOf(contestId)
        }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val imageRepository: ImageRepository = koinInject()

        val expressiveShapes = remember {
            listOf(
                MaterialShapes.Pentagon,
                MaterialShapes.Flower,
                MaterialShapes.Burst,
                MaterialShapes.Cookie4Sided,
                MaterialShapes.Clover4Leaf,
                MaterialShapes.Ghostish,
            )
        }

        val classVectors = remember(state.classes) {
            state.classes.map {
                Pair(it, expressiveShapes.random())
            }
        }

        val auth: Auth = koinInject()
        val currentUser = auth.currentUserOrNull()
        val isAdmin = currentUser.isAdmin

        val isError = state.contestUsers == null || state.contest == null
        val isJudge = currentUser?.isJudge(state.contestUsers.orEmpty()) ?: false

        val bonsaiList = state.bonsaiList
//        val sortedBonsaiList = remember(bonsaiList) {
//            if (state.contest?.state == "reviewing" && isJudge) bonsaiList?.sortedBy { it.id }
//
//            bonsaiList?.sortedBy { it.created_at }
//        }
        val reviews = state.reviews
        val mappedReviews = remember(reviews) {
            reviews.associateBy { it.bonsai_id }
        }

        val coroutineScope = rememberCoroutineScope()
        var dialog by remember { mutableStateOf<AlertDialogBuilder?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }
        val bottomSheetState = rememberBottomSheetState()

        BackHandler(enabled = bottomSheetState.currentValue == SheetPosition.Expanded) {
            coroutineScope.launch { bottomSheetState.animateTo(SheetPosition.HalfExpanded) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            KodamaScaffold(
                onNavigationIconClicked = { navigator?.pop() },
                appBarType = AppBarType.SMALL,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                actions = {
                    if (state.isLoading || state.isUpdatingState || isError) return@KodamaScaffold

                    when {
                        isAdmin && state.contest?.state == ContestState.Draft -> {
                            DropdownSplitButton(
                                leadingButton = {
                                    SplitButtonDefaults.LeadingButton(
                                        onClick = {
                                            if (!state.canFinalizeContest) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Minimal satu juri per kelas atau satu ketua juri diperlukan!")
                                                }
                                                return@LeadingButton
                                            }

                                            dialog = AlertDialogBuilder().apply {
                                                titleRes = Res.string.finalize_contest_confirm_title
                                                textRes = Res.string.finalize_contest_confirm_text
                                                confirmText = "Ya, Buka"
                                                cancelText = "Batal"
                                                onConfirm = {
                                                    dialog = null
                                                    viewModel.transitionContestState(
                                                        newState = "accepting",
                                                        onError = { error ->
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar(error)
                                                            }
                                                        },
                                                        onSuccess = {},
                                                    )
                                                }
                                                onCancel = { dialog = null }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (state.canFinalizeContest) {
                                                ButtonDefaults.buttonColors().containerColor
                                            } else {
                                                ButtonDefaults.buttonColors().disabledContainerColor
                                            },
                                            contentColor = if (state.canFinalizeContest) {
                                                ButtonDefaults.buttonColors().contentColor
                                            } else {
                                                ButtonDefaults.buttonColors().disabledContentColor
                                            }
                                        )
                                    ) {
                                        Text(stringResource(Res.string.finalize_contest))
                                    }
                                },
                                dropdownItems = { dismiss ->
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            dismiss()
                                        },
                                        leadingIcon = { Icon(edit, contentDescription = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Assign Judge") },
                                        onClick = {
                                            dismiss()
                                            navigator?.push(AssignJudgesScreen(contestId))
                                        },
                                        leadingIcon = { Icon(flag, contentDescription = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.delete_bonsai)) },
                                        onClick = {
                                            dismiss()
                                        },
                                        leadingIcon = { Icon(delete, contentDescription = null) },
                                    )
                                },
                            )
                        }
                        isAdmin && state.contest?.state == ContestState.Accepting -> {
                            Button(onClick = {
                                dialog = AlertDialogBuilder().apply {
                                    title = "Tutup pendaftaran?"
                                    text = "Users won't be able to register new bonsai."
                                    confirmText = "Ya, Tutup"
                                    cancelText = "Batal"
                                    onConfirm = {
                                        dialog = null
                                        viewModel.transitionContestState(
                                            newState = "closed",
                                            onError = { error ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                }
                                            },
                                            onSuccess = {},
                                        )
                                    }
                                    onCancel = { dialog = null }
                                }
                            }) {
                                Text("Close Registration")
                            }
                        }
                        isAdmin && state.contest?.state == ContestState.Closed -> {
                            Button(onClick = {
                                dialog = AlertDialogBuilder().apply {
                                    title = "Start review phase?"
                                    text = "Allow judges to start reviewing contestants' bonsai."
                                    confirmText = "Ya, Mulai"
                                    cancelText = "Batal"
                                    onConfirm = {
                                        dialog = null
                                        viewModel.transitionContestState(
                                            newState = "reviewing",
                                            onError = { error ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                }
                                            },
                                            onSuccess = {},
                                        )
                                    }
                                    onCancel = { dialog = null }
                                }
                            }) {
                                Text("Start Reviewing Phase")
                            }
                        }
                        isAdmin && state.contest?.state == ContestState.Reviewing -> {
                            Button(onClick = {
                                dialog = AlertDialogBuilder().apply {
                                    title = "Close review?"
                                    text = "Judges won't be able to review any more bonsai."
                                    confirmText = "Ya, Tutup"
                                    cancelText = "Batal"
                                    onConfirm = {
                                        dialog = null
                                        viewModel.transitionContestState(
                                            newState = "review_done",
                                            onError = { error ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                }
                                            },
                                            onSuccess = {},
                                        )
                                    }
                                    onCancel = { dialog = null }
                                }
                            }) {
                                Text("Close Review")
                            }
                        }
                    }
                },
            ) { contentPadding ->
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                    return@KodamaScaffold
                }

                if (isError) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Unable to load contest")
                        Button(onClick = { viewModel.loadContest() }) {
                            Text("Try again")
                        }
                    }
                    return@KodamaScaffold
                }

                val contest = state.contest ?: return@KodamaScaffold

                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AsyncImage(
                            model = imageRepository.getPublicUrl(contest),
                            contentDescription = "Contest banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ContestBannerRatio),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(alternate_email),
                            imageLoader = koinInject(),
                        )

                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    Chip(contest.state.name, contest.state.symbol)
                                }
                                items(classVectors) {
                                    Chip(it.first.name, fallbackShape = it.second.toShape())
                                }
                            }
                            Text(
                                text = contest.name,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = contest.created_at?.let {
                                    Instant
                                        .parse(it)
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                        .format(DateTimeFormat)
                                } ?: "",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = contest.description ?: "No description.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                }
            }

            if (isError) return@Box

            dialog?.Content()

            state.contest?.let { contest ->
                if (contest.state == ContestState.Draft) return@let
                // Wouldn't be fair to have judge able to join the contest now is it?
                if (contest.state == ContestState.Accepting && isJudge) return@let

                KodamaBottomSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    state = bottomSheetState,
                    dragHandleToolTipString = "Bonsai List",
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        // Not sure whether I should let admin register their bonsai or not, but it makes more sense not to I feel like.
                        if (contest.state == ContestState.Accepting && !currentUser.isAdmin) {
                            item(key = "bottom_sheet_add") {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        navigator?.push(CreateBonsaiScreen(contestId, state.classes.map { it.id }))
                                    },
                                    content = {
                                        Text("Daftarkan Bonsai")
                                    },
                                )
                            }
                            item { Spacer(Modifier.height(6.dp)) }
                        }

                        val isReviewing = contest.state == ContestState.Reviewing
                        if (state.isSheetLoading) {
                            item(key = "bottom_sheet_loading") {
                                Box(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                    LoadingIndicator(Modifier.align(Alignment.Center))
                                }
                            }
                        } else {
                            itemsIndexed(bonsaiList) { index, bonsai ->
                                val review = mappedReviews[bonsai.id]
                                val hasBeenReviewed = review != null
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = currentUser?.let { bonsai.owner_id == it.id } ?: false) {
                                            navigator?.push(BonsaiDetailScreen(contestId, bonsai.id))
                                        },
                                    shape = when {
                                        bonsaiList.size == 1 -> RoundedCornerShape(16.dp)
                                        index <= 0 -> RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 16.dp, topEnd = 16.dp)
                                        index >= bonsaiList.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)
                                        else -> RoundedCornerShape(4.dp)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = bonsai.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        when {
                                            isAdmin -> bonsai.AdminAction(
                                                viewModelState = state,
                                                isReviewing = isReviewing,
                                                onBonsaiVerify = { bonsai -> viewModel.verifyBonsai(bonsai.id) },
                                            )
                                            isJudge -> bonsai.JudgeAction(
                                                isReviewing = isReviewing,
                                                hasBeenReviewed = hasBeenReviewed,
                                            )
                                            else -> bonsai.UserAction(
                                                viewModelState = state,
                                                isReviewing = isReviewing,
                                                onBonsaiDelete = { bonsai ->
                                                    dialog = AlertDialogBuilder().apply {
                                                        titleRes = Res.string.delete_bonsai_confirm_title
                                                        textRes = Res.string.delete_bonsai_confirm_text
                                                        confirmText = "Hapus"
                                                        cancelText = "Batal"
                                                        onConfirm = {
                                                            viewModel.deleteBonsai(bonsai.id)
                                                            dialog = null
                                                        }
                                                        onCancel = { dialog = null }
                                                    }
                                                },
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
    }

    @Composable
    fun Bonsai.UserAction(
        viewModelState: ContestViewModel.State,
        isReviewing: Boolean,
        onBonsaiDelete: (Bonsai) -> Unit,
    ) {
        val navigator = LocalNavigator.current

        val bonsaiReviews = viewModelState.reviews.filter { it.bonsai_id == id }
        val contestUsers = viewModelState.contestUsers.orEmpty()
        val judgesForClass = contestUsers.count {
            it.role == "judge" && it.contest_class_id == contest_class_id
        } + contestUsers.count { it.role == "head_judge" }
        val votedCount = bonsaiReviews.size

        val isFullyVoted = votedCount == judgesForClass
        val flagPotential = if (isFullyVoted) bonsaiReviews.getFlagPotential(judgesForClass.toLong()) else 0

        when {
            !isReviewing && state == "draft" -> {
                DropdownSplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(onClick = { navigator?.push(FinalizeEntryScreen(contestId, id)) }) {
                            Text(stringResource(Res.string.finalize_bonsai))
                        }
                    },
                    dropdownItems = { dismiss ->
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                // TODO: Navigate to edit screen
                                dismiss()
                            },
                            leadingIcon = { Icon(edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete_bonsai)) },
                            onClick = {
                                onBonsaiDelete(this@UserAction)
                                dismiss()
                            },
                            leadingIcon = { Icon(delete, contentDescription = null) },
                        )
                    },
                )
            }
            !isReviewing && state == "waiting_verify" -> {
                Chip("Waiting to be verified", schedule)
            }
            !isReviewing && state == "verified" -> {
                Chip("Verified", verified)
            }
            isReviewing && isFullyVoted && flagPotential >= 200L -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(if (flagPotential >= 350L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        modifier = Modifier.padding(horizontal = 6.dp).size(20.dp),
                        imageVector = flag,
                        contentDescription = "Chip icon",
                        tint = if (flagPotential >= 350L) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    @Composable
    fun Bonsai.AdminAction(
        viewModelState: ContestViewModel.State,
        isReviewing: Boolean,
        onBonsaiVerify: (Bonsai) -> Unit,
    ) {
        val supabaseClient: SupabaseClient = koinInject()
        val supabaseUrl = supabaseClient.config.supabaseUrl

        val uriHandler = LocalUriHandler.current

        val bonsaiReviews = viewModelState.reviews.filter { it.bonsai_id == id }
        val contestUsers = viewModelState.contestUsers.orEmpty()
        val judgesForClass = contestUsers.count {
            it.role == "judge" && it.contest_class_id == contest_class_id
        } + contestUsers.count { it.role == "head_judge" }
        val votedCount = bonsaiReviews.size

        when {
            !isReviewing && state == "waiting_verify" -> {
                DropdownSplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(onClick = {
                            val proofUrl = payment_proof_path?.let { "https://$supabaseUrl/storage/v1/object/public/kodama-images/$it" } ?: return@LeadingButton
                            uriHandler.openUri(proofUrl)
                        }) {
                            Text("Buka bukti bayar")
                        }
                    },
                    dropdownItems = { dismiss ->
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.verify_bonsai)) },
                            onClick = {
                                dismiss()
                                onBonsaiVerify(this@AdminAction)
                            },
                            leadingIcon = { Icon(edit, contentDescription = null) },
                        )
                    },
                )
            }
            !isReviewing && state == "verified" -> {
                Chip("Verified", verified)
            }
            isReviewing -> {
                Chip(
                    text = stringResource(Res.string.judges_voted_format, votedCount, judgesForClass),
                    icon = reviews,
                )
            }
        }
    }

    @Composable
    fun Bonsai.JudgeAction(
        isReviewing: Boolean,
        hasBeenReviewed: Boolean,
    ) {
        if (!isReviewing) return

        when {
            hasBeenReviewed -> {
                Chip(stringResource(Res.string.voted), reviews)
            }
            !hasBeenReviewed -> {
                Chip("Not yet rated", rate_review)
            }
        }
    }
}

val DateTimeFormat = LocalDateTime.Format {
    day(Padding.NONE)                         // Prints day without a leading zero (e.g., "1")
    char(' ')                            // Space delimiter
    monthName(MonthNames.ENGLISH_FULL)   // Prints full month name (e.g., "January")
    char(' ')                            // Space delimiter
    year()                               // Prints full year (e.g., "2024")
}

val ContestBannerRatio = 16f / 9f

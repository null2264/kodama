package kodama.ui.presentation.contest

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.user.UserInfo
import kodama.core.data.Bonsai
import kodama.core.data.ImageRepository
import kodama.core.util.isAdmin
import kodama.core.util.isJudge
import kodama.resources.Res
import kodama.resources.finalize_bonsai
import kodama.resources.finalize_contest
import kodama.resources.finalize_contest_confirm_text
import kodama.resources.finalize_contest_confirm_title
import kodama.resources.icons.account_circle
import kodama.resources.icons.alternate_email
import kodama.resources.icons.delete
import kodama.resources.icons.edit
import kodama.resources.verify_bonsai
import kodama.resources.voted
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.Chip
import kodama.ui.component.KodamaBottomSheet
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.LoadingButton
import kodama.ui.presentation.bonsai.BonsaiDetailScreen
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
import kotlin.time.Instant

internal class ContestScreen(
    private val contestId: String,
) : Screen() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
        val isJudge = currentUser?.isJudge(state.contestUsers) ?: false

        val bonsaiList by viewModel.subscribeBonsaiList().collectAsState(null)
//        val sortedBonsaiList = remember(bonsaiList) {
//            if (state.contest?.state == "reviewing" && isJudge) bonsaiList?.sortedBy { it.id }
//
//            bonsaiList?.sortedBy { it.created_at }
//        }
        val reviews by viewModel.subscribeReviews().collectAsState(null)
        val mappedReviews = remember(reviews) {
            reviews?.associateBy { it.bonsai_id }
        }

        val coroutineScope = rememberCoroutineScope()
        var dialog by remember { mutableStateOf<AlertDialogBuilder?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }

        Box(modifier = Modifier.fillMaxSize()) {
            KodamaScaffold(
                onNavigationIconClicked = { navigator?.pop() },
                appBarType = AppBarType.SMALL,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                actions = {
                    if (state.isLoading) return@KodamaScaffold

                    if (isAdmin && state.contest?.state == "draft" && !state.isUpdatingState) {
                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.LeadingButton(onClick = {
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
                                }) {
                                    Text(stringResource(Res.string.finalize_contest))
                                }
                            },
                            trailingButton = {
                                SplitButtonDefaults.TrailingButton(
                                    checked = false,
                                    onCheckedChange = {},
                                ) {
                                    Icon(
                                        edit,
                                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                                        contentDescription = "Localized description"
                                    )
                                }
                            }
                        )
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

                if (state.contest == null) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(contentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Unable to load contest")
                        Button(
                            onClick = { viewModel.loadContest() },
                        ) {
                            Text("Try again")
                        }
                    }
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
                                item { Chip(contest.state.replaceFirstChar { it.uppercase() }, account_circle) }
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

            dialog?.Content()

            state.contest?.let { contest ->
                if (contest.state == "draft") return@let
                // Wouldn't be fair to have judge able to join the contest now is it?
                if (contest.state == "accepting" && isJudge) return@let

                KodamaBottomSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    dragHandleToolTipString = "Bonsai List",
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        // Not sure whether I should let admin register their bonsai or not, but it makes more sense not to I feel like.
                        if (contest.state == "accepting" && !currentUser.isAdmin) {
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
                            item { Spacer(Modifier.height(4.dp)) }
                        }

                        val isReviewing = contest.state == "reviewing"
                        val isReviewingButReviewsIsLoading = isReviewing && isJudge && reviews == null
                        if (bonsaiList == null || isReviewingButReviewsIsLoading) {
                            item(key = "bottom_sheet_loading") {
                                Box(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                    LoadingIndicator(Modifier.align(Alignment.Center))
                                }
                            }
                        } else {
                            itemsIndexed(bonsaiList.orEmpty()) { index, bonsai ->
                                val review = mappedReviews?.get(bonsai.id)
                                val hasBeenReviewed = review != null
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            navigator?.push(BonsaiDetailScreen(contestId, bonsai.id))
                                        },
                                    shape = when {
                                        bonsaiList.orEmpty().size == 1 -> RoundedCornerShape(16.dp)
                                        index <= 0 -> RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 16.dp, topEnd = 16.dp)
                                        index >= bonsaiList.orEmpty().size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)
                                        else -> RoundedCornerShape(4.dp)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        bonsai.Action(
                                            viewModelState = state,
                                            currentUser = currentUser,
                                            isReviewing = isReviewing,
                                            hasBeenReviewed = hasBeenReviewed,
                                            onBonsaiVerify = { bonsai -> viewModel.verifyBonsai(bonsai.id) },
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
    fun Bonsai.Action(
        viewModelState: ContestViewModel.State,
        currentUser: UserInfo?,
        isReviewing: Boolean,
        hasBeenReviewed: Boolean,
        onBonsaiVerify: (Bonsai) -> Unit,
    ) {
        val navigator = LocalNavigator.current

        when {
            !isReviewing && state == "draft" && currentUser?.isJudge(viewModelState.contestUsers) != true -> {
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(onClick = { navigator?.push(FinalizeEntryScreen(contestId, id)) }) {
                            Text(stringResource(Res.string.finalize_bonsai))
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TrailingButton(
                            checked = false,
                            onCheckedChange = {},
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Icon(
                                delete,
                                modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                                contentDescription = "Hapus"
                            )
                        }
                    }
                )
            }
            !isReviewing && state == "waiting_verify" && currentUser?.isAdmin == true -> {
                LoadingButton(
                    onClick = { onBonsaiVerify(this) },
                    isLoading = viewModelState.bonsaiIsVerifying.getOrDefault(id, false),
                ) {
                    Text(stringResource(Res.string.verify_bonsai))
                }
            }
            !isReviewing && hasBeenReviewed -> {
                Text(
                    text = stringResource(Res.string.voted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            !isReviewing && !hasBeenReviewed -> {
                TextButton(
                    onClick = { /*onRateBonsai(bonsai.id)*/ },
                ) {
                    Text("Rate")
                }
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

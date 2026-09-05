package kodama.ui.presentation.contest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import kodama.core.data.ImageRepository
import kodama.resources.Res
import kodama.resources.add_contest
import kodama.resources.contest_detail_title
import kodama.resources.icons.account_circle
import kodama.resources.icons.add
import kodama.resources.icons.alternate_email
import kodama.ui.component.AppBarType
import kodama.ui.component.Chip
import kodama.ui.component.KodamaScaffold
import kodama.ui.presentation.contest.slop.CreateBonsaiScreen
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.random.Random
import kotlin.time.Instant

internal class ContestScreen(
    private val contestId: String,
) : Screen() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<ContestScreenModel> {
            parametersOf(contestId)
        }
        val state by screenModel.state.collectAsState()
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

        val sheetState = rememberBottomSheetState(
            enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
            initialValue = SheetValue.PartiallyExpanded,
            confirmValueChange = { it != SheetValue.Hidden },
        )

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            appBarType = AppBarType.SMALL,
//            snackbarHost = { SnackbarHost(snackbarHostState) },
            actions = {
//                if (isAdmin && state.contest?.state == "draft") {
//                    ToolTipButton(
//                        toolTipLabel = "Edit",
//                        icon = edit,
//                        buttonClicked = { navigator?.push(EditContestScreen(contestId)) },
//                    )
//                }
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
                        onClick = { screenModel.loadContest() },
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
                            .aspectRatio(215f / 54f),
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

                BottomSheet(
                    state = sheetState,
                ) {
                    Text("Hello dingus")
                }

                FloatingActionButton(
                    onClick = {
                        navigator?.push(CreateBonsaiScreen(contestId, state.classes.map { it.id }))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        painter = rememberVectorPainter(add),
                        contentDescription = stringResource(Res.string.add_contest),
                    )
                }
            }
        }
    }
}

val DateTimeFormat = LocalDateTime.Format {
    day()                         // Prints day without a leading zero (e.g., "1")
    char(' ')                            // Space delimiter
    monthName(MonthNames.ENGLISH_FULL)   // Prints full month name (e.g., "January")
    char(' ')                            // Space delimiter
    year()                               // Prints full year (e.g., "2024")
}

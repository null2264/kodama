package kodama.ui.presentation.contest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kodama.resources.icons.edit
import kodama.ui.component.AppBarType
import kodama.ui.component.Chip
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.ToolTipButton
import kodama.ui.presentation.contest.slop.CreateContestScreen
import kodama.ui.presentation.contest.slop.EditContestScreen
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class ContestScreen(
    private val contestId: String,
) : Screen() {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<ContestScreenModel> {
            parametersOf(contestId)
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val imageRepository: ImageRepository = koinInject()

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = state.contest?.name ?: stringResource(Res.string.contest_detail_title),
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
                    CircularProgressIndicator()
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
                    ) {
                        Chip(contest.state, account_circle)
                        Text(contest.name)
                        Text(contest.description ?: "No description.")
                    }
                }

                FloatingActionButton(
                    onClick = { navigator?.parent?.push(CreateContestScreen()) },
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

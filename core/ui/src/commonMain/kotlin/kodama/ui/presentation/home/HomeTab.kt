package kodama.ui.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import io.github.jan.supabase.auth.Auth
import kodama.resources.icons.add
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kodama.core.util.kodamaRole
import kodama.resources.Res
import kodama.resources.add_contest
import kodama.resources.icons.alternate_email
import kodama.resources.icons.home
import kodama.resources.no_open_contests
import kodama.ui.presentation.contest.ContestDetailScreen
import kodama.ui.presentation.contest.CreateContestScreen
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal object HomeTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(home)

            return remember {
                TabOptions(
                    index = 0u,
                    title = "Explore",
                    icon = icon,
                )
            }
        }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val contestRepository: ContestRepository = koinInject()
        val auth: Auth = koinInject()
        val navigator = LocalNavigator.current

        val isAdmin = auth.currentUserOrNull()?.kodamaRole == "admin"

        var contests by remember { mutableStateOf<List<Contest>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        LaunchedEffect(Unit) {
            isLoading = true
            error = null
            try {
                contests = contestRepository.getOpenContests()
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }

        val visibleContests = if (isAdmin) contests else contests.filter { it.state != "draft" }

        val filteredContests = visibleContests.filter { contest ->
            val matchesSearch = searchQuery.isBlank() || contest.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Registration" -> contest.state == "accepting"
                "On-going" -> contest.state == "reviewing"
                "Ended" -> contest.state == "ended" || contest.state == "finished"
                else -> true
            }
            matchesSearch && matchesFilter
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Search contests",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val filters = listOf("All", "Location", "Registration", "On-going", "Ended")
                        filters.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    filteredContests.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.no_open_contests),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            items(filteredContests, key = { it.id }) { contest ->
                                ContestCard(contest = contest) {
                                    navigator?.parent?.push(ContestDetailScreen(contest.id))
                                }
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navigator?.parent?.push(CreateContestScreen()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = rememberVectorPainter(add),
                        contentDescription = stringResource(Res.string.add_contest)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContestCard(contest: Contest, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val imageRepository: ImageRepository = koinInject()
            AsyncImage(
                model = imageRepository.getPublicUrl(contest),
                contentDescription = "Contest banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(215f / 54f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(alternate_email),
                imageLoader = ImageLoader(LocalPlatformContext.current),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = contest.name,
                    style = MaterialTheme.typography.titleLarge
                )

                contest.description?.let { description ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contest.state.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

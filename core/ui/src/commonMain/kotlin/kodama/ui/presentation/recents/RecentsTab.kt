package kodama.ui.presentation.recents

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
import kodama.core.data.Contest
import kodama.core.data.ContestRepository
import kodama.core.data.ImageRepository
import kodama.resources.icons.alternate_email
import kodama.resources.icons.schedule
import kodama.ui.presentation.contest.ContestDetailScreen
import org.koin.compose.koinInject

internal object RecentsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(schedule)

            return remember {
                TabOptions(
                    index = 1u,
                    title = "Joined",
                    icon = icon,
                )
            }
        }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val contestRepository: ContestRepository = koinInject()
        val navigator = LocalNavigator.current

        var contests by remember { mutableStateOf<List<Contest>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var selectedFilter by remember { mutableStateOf("All") }

        LaunchedEffect(Unit) {
            isLoading = true
            error = null
            try {
                contests = contestRepository.getJoinedContests()
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }

        val filteredContests = contests.filter { contest ->
            when (selectedFilter) {
                "Registration" -> contest.state == "accepting"
                "On-going" -> contest.state == "reviewing"
                "Ended" -> contest.state == "ended" || contest.state == "finished"
                else -> true
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Kontes Diikuti",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val filters = listOf("All", "On-going", "Ended")
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

            Spacer(modifier = Modifier.height(8.dp))

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
                        text = "Belum ada kontes yang diikuti",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filteredContests, key = { it.id }) { contest ->
                            JoinedContestCard(contest = contest) {
                                navigator?.parent?.push(ContestDetailScreen(contest.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinedContestCard(contest: Contest, onClick: () -> Unit) {
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

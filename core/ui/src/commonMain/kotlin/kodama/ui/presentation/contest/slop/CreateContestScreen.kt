package kodama.ui.presentation.contest.slop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import kodama.resources.icons.upload
import kodama.resources.Res
import kodama.resources.change_banner
import kodama.resources.contest_classes_label
import kodama.resources.contest_desc_label
import kodama.resources.contest_desc_placeholder
import kodama.resources.contest_name_label
import kodama.resources.contest_name_placeholder
import kodama.resources.create_contest_submit
import kodama.resources.create_contest_title
import kodama.resources.pick_banner
import kodama.resources.icons.alternate_email
import kodama.resources.icons.check
import kodama.resources.icons.edit
import kodama.ui.component.AlertDialogBuilder
import kodama.ui.component.AppBarType
import kodama.ui.component.KodamaScaffold
import kodama.ui.component.KodamaTextField
import kodama.ui.component.LoadingButton
import kodama.ui.component.rememberImageFilePicker
import kodama.ui.presentation.utils.Screen
import kodama.ui.presentation.utils.rememberScreenModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal class CreateContestScreen : Screen() {

    @OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<CreateContestScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.current
        val coroutineScope = rememberCoroutineScope()
        val filePicker = rememberImageFilePicker()
        val keyboardController = LocalSoftwareKeyboardController.current

        var alertDialogMessage by remember { mutableStateOf<String?>(null) }

        KodamaScaffold(
            onNavigationIconClicked = { navigator?.pop() },
            title = stringResource(Res.string.create_contest_title),
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                BannerImagePicker(
                    previewBytes = state.bannerPreviewBytes,
                    onPick = {
                        coroutineScope.launch {
                            val bytes = filePicker.pick()
                            if (bytes != null) {
                                screenModel.onBannerPicked(bytes)
                            }
                        }
                    },
                )

                KodamaTextField(
                    value = state.name,
                    onValueChange = { screenModel.onNameChanged(it) },
                    label = stringResource(Res.string.contest_name_label),
                    placeholder = stringResource(Res.string.contest_name_placeholder),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    icon = { Icon(alternate_email, contentDescription = null, modifier = Modifier.size(24.dp)) },
                )

                KodamaTextField(
                    value = state.description,
                    onValueChange = { screenModel.onDescriptionChanged(it) },
                    label = stringResource(Res.string.contest_desc_label),
                    placeholder = stringResource(Res.string.contest_desc_placeholder),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    icon = { Icon(alternate_email, contentDescription = null, modifier = Modifier.size(24.dp)) },
                )

                Column {
                    Text(
                        text = stringResource(Res.string.contest_classes_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.availableClasses.forEach { bonsaiClass ->
                            val isSelected = bonsaiClass.id in state.selectedClassIds
                            FilterChip(
                                selected = isSelected,
                                leadingIcon = { if (isSelected) { Icon(check, "Selected") } },
                                onClick = { screenModel.toggleClass(bonsaiClass.id) },
                                label = { Text(bonsaiClass.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }

                LoadingButton(
                    onClick = {
                        keyboardController?.hide()
                        screenModel.createContest(
                            onError = { alertDialogMessage = it },
                            onSuccess = { contestId ->
                                navigator?.replace(ContestDetailScreen(contestId, showCreatedSnackbar = true))
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    isLoading = state.isLoading,
                    enabled = state.name.isNotBlank() && state.selectedClassIds.isNotEmpty() && !state.isLoading,
                ) {
                    Text(stringResource(Res.string.create_contest_submit))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            alertDialogMessage?.let { message ->
                AlertDialogBuilder().apply {
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
private fun BannerImagePicker(
    previewBytes: ByteArray?,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(215f / 54f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onPick() },
        contentAlignment = Alignment.Center,
    ) {
        if (previewBytes != null) {
            AsyncImage(
                model = previewBytes,
                contentDescription = stringResource(Res.string.change_banner),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                imageLoader = koinInject(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.change_banner),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = upload,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.pick_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

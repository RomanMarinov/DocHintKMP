package app.romanmarinov.dochintkmp.presentation.documents_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsEvent
import app.romanmarinov.dochintkmp.presentation.text.russianAnalysisCountLabel
import app.romanmarinov.dochintkmp.presentation.ui.AppTopBar
import org.koin.androidx.compose.koinViewModel

/** Запас снизу списка: FAB + отступы (над системной навигацией уже учтён [Scaffold] в [AppNavigation]). */
private val DocumentsListBottomInsetFab = 88.dp

@Composable
fun ResultsScreen(viewModel: ResultsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var deleteIndex by remember { mutableIntStateOf(-1) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIndices = emptySet()
    }

    fun toggleIndex(index: Int) {
        selectedIndices = if (index in selectedIndices) selectedIndices - index else selectedIndices + index
    }

    if (deleteIndex >= 0) {
        AlertDialog(
            onDismissRequest = { deleteIndex = -1 },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(ResultsEvent.RemoveAt(deleteIndex))
                    deleteIndex = -1
                    exitSelectionMode()
                }) {
                    Text(stringResource(R.string.dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = -1 }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(ResultsEvent.ClearResults)
                    showClearDialog = false
                    exitSelectionMode()
                }) {
                    Text(stringResource(R.string.dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    val subtitle = when {
        uiState.results.isEmpty() -> stringResource(R.string.no_saved_analyses)
        selectionMode -> stringResource(R.string.documents_selected_count, selectedIndices.size)
        else -> russianAnalysisCountLabel(uiState.results.size)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.screen_documents),
                subtitle = subtitle,
                actions = {
                    if (uiState.results.isNotEmpty()) {
                        if (selectionMode) {
                            TextButton(onClick = { exitSelectionMode() }) {
                                Text(stringResource(R.string.documents_selection_cancel))
                            }
                        } else {
                            FilledTonalButton(onClick = { showClearDialog = true }) {
                                Text(stringResource(R.string.all_clear_data))
                            }
                        }
                    }
                }
            )

            if (uiState.results.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = DocumentsListBottomInsetFab
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.results, key = { i, _ -> i }) { index, data ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { it / 2 } + fadeIn()
                        ) {
                            ResultCard(
                                data = data,
                                index = index,
                                selectionMode = selectionMode,
                                selected = index in selectedIndices,
                                onToggleSelect = { toggleIndex(index) },
                                onDelete = { deleteIndex = index }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.results.isNotEmpty()) {
            val pickPhase = selectionMode && selectedIndices.isEmpty()
            ExtendedFloatingActionButton(
                onClick = {
                    when {
                        !selectionMode -> {
                            selectionMode = true
                            selectedIndices = emptySet()
                        }
                        selectedIndices.isNotEmpty() -> { /* экспорт позже */ }
                        else -> { /* выбор в списке — см. подзаголовок «Выбрано» */ }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.content_desc_share)
                    )
                },
                text = {
                    Text(
                        when {
                            !selectionMode -> stringResource(R.string.documents_action_send)
                            pickPhase -> stringResource(R.string.documents_fab_pick_first)
                            else -> stringResource(R.string.documents_share) + " (${selectedIndices.size})"
                        }
                    )
                },
                expanded = true
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FolderOpen, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultCard(
    data: MedicalData,
    index: Int,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (selectionMode) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (selectionMode) Modifier.clickable(onClick = onToggleSelect)
                                else Modifier
                            )
                    ) {
                        Text(
                            stringResource(R.string.label_document_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            data.documentType ?: stringResource(R.string.analysis_default),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        data.analysisDate?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (!selectionMode) {
                    IconButton(onClick = onDelete) {
                        Icon(painter = painterResource(R.drawable.ic_delete), stringResource(R.string.content_desc_delete), Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            data.institution?.let { MetaRow(title = stringResource(R.string.label_institution), institution = it) }
            data.doctorName?.let { MetaRow(title = stringResource(R.string.label_doctor), institution = it) }

            data.indicators?.takeIf { it.isNotEmpty() }?.let { indicators ->
                Spacer(modifier = Modifier.height(12.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(R.string.indicators_short_format, indicators.size),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        indicators.forEach { ind ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    ind.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    ind.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                ind.referenceRange?.let {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
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
private fun MetaRow(title: String, institution: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(institution, style = MaterialTheme.typography.bodyMedium)
    }
}

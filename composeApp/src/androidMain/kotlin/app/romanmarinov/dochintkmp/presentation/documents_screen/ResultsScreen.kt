package app.romanmarinov.dochintkmp.presentation.documents_screen

import android.content.ClipData
import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.data.util.isoUtcToMoscowDateTimeOrSelf
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.DocumentsShareFab
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.DocumentsTab
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ImportByCodeSummary
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsEffect
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsIntent
import app.romanmarinov.dochintkmp.presentation.documents_screen.tabs.DocumentsTabContent
import app.romanmarinov.dochintkmp.presentation.documents_screen.tabs.ReceivedTabContent
import app.romanmarinov.dochintkmp.presentation.documents_screen.util.toImportSummaryMessage
import app.romanmarinov.dochintkmp.presentation.text.russianAnalysisCountLabel
import app.romanmarinov.dochintkmp.presentation.ui.AppAlertDialog
import app.romanmarinov.dochintkmp.presentation.ui.AppTopBar
import org.koin.androidx.compose.koinViewModel

private val DocumentsListBottomInsetFab = 88.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResultsScreen(viewModel: ResultsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ownIndexed = uiState.results.withIndex().filter { it.value.source != ResultsRepository.SOURCE_SHARED }
    val ownItems = ownIndexed.map { it.value }
    val receivedIndexed = uiState.results.withIndex().filter { it.value.source == ResultsRepository.SOURCE_SHARED }
    val receivedItems = receivedIndexed.map { it.value }
    val ownSelectedVisibleIndices = ownIndexed.mapIndexedNotNull { visibleIndex, indexed ->
        if (indexed.index in uiState.selectedIndices) visibleIndex else null
    }.toSet()

    var actionErrorEffect by remember { mutableStateOf<ResultsEffect.Error?>(null) }
    var importSummaryData by remember { mutableStateOf<ImportByCodeSummary?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val actionErrorMessage = actionErrorEffect?.let { effect ->
        effect.message ?: stringResource(effect.messageRes)
    }
    val importSummaryMessage = importSummaryData?.toImportSummaryMessage()

    CollectResultsEffects(
        viewModel = viewModel,
        onError = { actionErrorEffect = it },
        onImportSummary = { importSummaryData = it }
    )

    AppAlertDialog(
        visible = uiState.deleteIndex != null,
        title = stringResource(R.string.dialog_delete_title),
        message = stringResource(R.string.dialog_delete_message),
        onDismissRequest = { viewModel.onIntent(ResultsIntent.DismissDeleteDialog) },
        onConfirm = { viewModel.onIntent(ResultsIntent.ConfirmDelete) },
        confirmText = stringResource(R.string.dialog_confirm),
        dismissText = stringResource(R.string.dialog_cancel),
        confirmTextColor = MaterialTheme.colorScheme.error
    )

    AppAlertDialog(
        visible = uiState.showClearDialog,
        title = stringResource(R.string.dialog_clear_title),
        message = stringResource(R.string.dialog_clear_message),
        onDismissRequest = { viewModel.onIntent(ResultsIntent.DismissClearDialog) },
        onConfirm = { viewModel.onIntent(ResultsIntent.ConfirmClear) },
        confirmText = stringResource(R.string.dialog_confirm),
        dismissText = stringResource(R.string.dialog_cancel),
        confirmTextColor = MaterialTheme.colorScheme.error
    )

    AppAlertDialog(
        visible = actionErrorMessage != null,
        title = stringResource(R.string.dialog_error_title),
        message = actionErrorMessage.orEmpty(),
        onDismissRequest = { actionErrorEffect = null },
        onConfirm = { actionErrorEffect = null },
        confirmText = stringResource(R.string.dialog_ok),
        dismissText = null
    )

    AppAlertDialog(
        visible = importSummaryMessage != null,
        title = stringResource(R.string.dialog_import_result_title),
        message = importSummaryMessage.orEmpty(),
        onDismissRequest = { importSummaryData = null },
        onConfirm = { importSummaryData = null },
        confirmText = stringResource(R.string.dialog_ok),
        dismissText = null
    )

    val subtitle = when {
        uiState.selectedTab == DocumentsTab.MINE && ownItems.isEmpty() -> stringResource(R.string.no_saved_analyses)
        uiState.selectedTab == DocumentsTab.RECEIVED && receivedItems.isEmpty() -> stringResource(R.string.documents_no_imported)
        uiState.selectionMode -> stringResource(R.string.documents_selected_count, uiState.selectedIndices.size)
        else -> russianAnalysisCountLabel(if (uiState.selectedTab == DocumentsTab.MINE) ownItems.size else receivedItems.size)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    focusManager.clearFocus()
                }
                false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.screen_documents),
                subtitle = subtitle,
                actions = {
                    if (uiState.selectedTab == DocumentsTab.MINE && ownItems.isNotEmpty()) {
                        if (uiState.selectionMode) {
                            TextButton(onClick = { viewModel.onIntent(ResultsIntent.SetSelectionMode(false)) }) {
                                Text(stringResource(R.string.documents_selection_cancel))
                            }
                        } else {
                            FilledTonalButton(onClick = { viewModel.onIntent(ResultsIntent.ShowClearDialog) }) {
                                Text(stringResource(R.string.all_clear_data))
                            }
                        }
                    }
                }
            )

            SecondaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == DocumentsTab.MINE,
                    onClick = { viewModel.onIntent(ResultsIntent.SelectTab(DocumentsTab.MINE)) },
                    text = { Text(stringResource(R.string.documents_tab_mine)) }
                )
                Tab(
                    selected = uiState.selectedTab == DocumentsTab.RECEIVED,
                    onClick = { viewModel.onIntent(ResultsIntent.SelectTab(DocumentsTab.RECEIVED)) },
                    text = { Text(stringResource(R.string.documents_tab_received)) }
                )
            }

            if (uiState.selectedTab == DocumentsTab.MINE) {
                DocumentsTabContent(
                    modifier = Modifier.fillMaxSize(),
                    items = ownItems,
                    selectionMode = uiState.selectionMode,
                    selectedIndices = ownSelectedVisibleIndices,
                    bottomInset = DocumentsListBottomInsetFab,
                    onToggleSelect = { index ->
                        ownIndexed.getOrNull(index)?.index?.let {
                            viewModel.onIntent(ResultsIntent.ToggleSelect(it))
                        }
                    },
                    onDelete = { index ->
                        ownIndexed.getOrNull(index)?.index?.let {
                            viewModel.onIntent(ResultsIntent.DeleteRequest(it))
                        }
                    }
                )
            } else {
                ReceivedTabContent(
                    modifier = Modifier.fillMaxSize(),
                    inputCode = uiState.inputCode,
                    networkBusy = uiState.networkBusy,
                    items = receivedItems,
                    onInputCodeChange = {
                        viewModel.onIntent(ResultsIntent.InputCodeChanged(it.trim().uppercase()))
                    },
                    onClick = { viewModel.onIntent(ResultsIntent.SendCode) },
                    onDelete = { index ->
                        receivedIndexed.getOrNull(index)?.index?.let {
                            viewModel.onIntent(ResultsIntent.DeleteRequest(it))
                        }
                    }
                )
            }
        }

        if (uiState.selectedTab == DocumentsTab.MINE && ownItems.isNotEmpty()) {
            DocumentsShareFab(
                selectionMode = uiState.selectionMode,
                selectedCount = uiState.selectedIndices.size,
                onClick = { viewModel.onIntent(ResultsIntent.FabClicked) }
            )
        }

        uiState.shareSheet?.let { shareSheet ->
            ModalBottomSheet(onDismissRequest = { viewModel.onIntent(ResultsIntent.DismissShareSheet) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.documents_share_code_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(shareSheet.code, style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(
                            R.string.documents_share_valid_until,
                            shareSheet.expiresAt.isoUtcToMoscowDateTimeOrSelf()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("share_code", shareSheet.code))
                    }) {
                        Text(stringResource(R.string.documents_copy_code))
                    }
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                }
            }
        }
    }
}

@Composable
private fun CollectResultsEffects(
    viewModel: ResultsViewModel,
    onError: (ResultsEffect.Error) -> Unit,
    onImportSummary: (ImportByCodeSummary) -> Unit
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ResultsEffect.Error -> onError(effect)
                is ResultsEffect.ImportSummary -> onImportSummary(effect.summary)
            }
        }
    }
}
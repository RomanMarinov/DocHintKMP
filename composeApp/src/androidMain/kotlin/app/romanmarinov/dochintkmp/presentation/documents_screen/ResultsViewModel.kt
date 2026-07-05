package app.romanmarinov.dochintkmp.presentation.documents_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.data.remote.DocumentsShareGateway
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.DocumentsTab
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ImportByCodeSummary
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsEffect
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsIntent
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsState
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ShareSheetState
import app.romanmarinov.dochintkmp.presentation.documents_screen.util.sameAs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResultsViewModel(
    private val observeResultsUseCase: ObserveResultsUseCase,
    private val removeResult: RemoveResultUseCase,
    private val clearResults: ClearResultsUseCase,
    private val addResult: AddResultUseCase,
    private val documentsShareGateway: DocumentsShareGateway
) : ViewModel() {

    private val _uiState: MutableStateFlow<ResultsState> = MutableStateFlow(ResultsState())
    val uiState: StateFlow<ResultsState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ResultsEffect>()
    val effects = _effects.asSharedFlow()

    init {
        observeUiState()
    }

    private fun observeUiState() {
        viewModelScope.launch {
            observeResultsUseCase().collect { results ->
                _uiState.update { it.copy(results = results) }
            }
        }
    }

    fun onIntent(intent: ResultsIntent) {
        when (intent) {
            is ResultsIntent.SelectTab -> handleSelectTab(intent.tab)
            is ResultsIntent.SetSelectionMode -> handleSetSelectionMode(intent.enabled)
            is ResultsIntent.ToggleSelect -> handleToggleSelect(intent.index)
            is ResultsIntent.DeleteRequest -> handleDeleteRequest(intent.index)
            is ResultsIntent.InputCodeChanged -> handleInputCodeChanged(intent.value)
            ResultsIntent.DismissDeleteDialog -> handleDismissDeleteDialog()
            ResultsIntent.ConfirmDelete -> handleConfirmDelete()
            ResultsIntent.ShowClearDialog -> handleShowClearDialog()
            ResultsIntent.DismissClearDialog -> handleDismissClearDialog()
            ResultsIntent.ConfirmClear -> handleConfirmClear()
            ResultsIntent.SendCode -> sendCode()
            ResultsIntent.FabClicked -> onFabClicked()
            ResultsIntent.DismissShareSheet -> handleDismissShareSheet()
        }
    }

    private fun handleSelectTab(tab: DocumentsTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    private fun handleSetSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                selectionMode = enabled,
                selectedIndices = if (enabled) it.selectedIndices else emptySet()
            )
        }
    }

    private fun handleToggleSelect(index: Int) {
        _uiState.update {
            val next = if (index in it.selectedIndices) {
                it.selectedIndices - index
            } else {
                it.selectedIndices + index
            }
            it.copy(selectedIndices = next)
        }
    }

    private fun handleDeleteRequest(index: Int) {
        _uiState.update { it.copy(deleteIndex = index) }
    }

    private fun handleDismissDeleteDialog() {
        _uiState.update { it.copy(deleteIndex = null) }
    }

    private fun handleConfirmDelete() {
        val index = _uiState.value.deleteIndex ?: return
        removeResult(index)
        _uiState.update {
            it.copy(
                deleteIndex = null,
                selectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    private fun handleShowClearDialog() {
        _uiState.update { it.copy(showClearDialog = true) }
    }

    private fun handleDismissClearDialog() {
        _uiState.update { it.copy(showClearDialog = false) }
    }

    private fun handleConfirmClear() {
        clearResults()
        _uiState.update {
            it.copy(
                showClearDialog = false,
                selectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    private fun handleInputCodeChanged(value: String) {
        _uiState.update { it.copy(inputCode = value) }
    }

    private fun handleDismissShareSheet() {
        _uiState.update { it.copy(shareSheet = null) }
    }

    fun addImportedResults(items: List<HousingPaymentDocument>) {
        items.forEach { addResult(it, processedText = null, source = ResultsRepository.SOURCE_SHARED) }
    }

    private suspend fun importByCodeInternal(code: String): ImportByCodeSummary {
        val items = documentsShareGateway.receiveDocumentsByCode(code)
        val receivedItems = uiState.value.results.filter { it.source == ResultsRepository.SOURCE_SHARED }
        val uniqueItems = items.filter { incoming ->
            receivedItems.none { existing -> existing.sameAs(incoming) }
        }
        val skipped = items.size - uniqueItems.size
        if (uniqueItems.isNotEmpty()) {
            addImportedResults(uniqueItems)
        }
        return ImportByCodeSummary(added = uniqueItems.size, skipped = skipped)
    }

    private fun sendCode() {
        val current = _uiState.value
        if (current.inputCode.isBlank() || current.networkBusy) return
        _uiState.update { it.copy(networkBusy = true) }
        viewModelScope.launch {
            runCatching {
                val code = _uiState.value.inputCode
                importByCodeInternal(code)
            }
                .onSuccess { summary ->
                    _uiState.update { it.copy(inputCode = "") }
                    _effects.emit(ResultsEffect.ImportSummary(summary))
                }
                .onFailure {
                    _effects.emit(
                        ResultsEffect.Error(
                            message = it.message,
                            messageRes = R.string.documents_import_failed
                        )
                    )
                }
            _uiState.update { it.copy(networkBusy = false) }
        }
    }

    private fun onFabClicked() {
        val state = _uiState.value
        if (state.selectedTab != DocumentsTab.MINE) return
        when {
            !state.selectionMode -> {
                _uiState.update { it.copy(selectionMode = true, selectedIndices = emptySet()) }
            }

            state.selectedIndices.isEmpty() -> {
                viewModelScope.launch {
                    _effects.emit(
                        ResultsEffect.Error(
                            message = null,
                            messageRes = R.string.documents_select_at_least_one
                        )
                    )
                }
            }

            else -> {
                _uiState.update { it.copy(networkBusy = true) }
                viewModelScope.launch {
                    runCatching {
                        val selectedItems = state.selectedIndices.toList().sorted().mapNotNull { idx ->
                            uiState.value.results.getOrNull(idx)
                        }
                        documentsShareGateway.sendDocuments(selectedItems)
                    }.onSuccess { response ->
                        _uiState.update {
                            it.copy(
                                shareSheet = ShareSheetState(
                                    code = response.code,
                                    expiresAt = response.expiresAt
                                )
                            )
                        }
                    }.onFailure {
                        _effects.emit(
                            ResultsEffect.Error(
                                message = it.message,
                                messageRes = R.string.documents_share_failed
                            )
                        )
                    }
                    _uiState.update { it.copy(networkBusy = false) }
                }
            }
        }
    }
}
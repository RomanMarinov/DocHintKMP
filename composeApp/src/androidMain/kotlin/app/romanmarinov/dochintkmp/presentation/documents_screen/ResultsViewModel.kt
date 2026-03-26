package app.romanmarinov.dochintkmp.presentation.documents_screen

import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsEvent
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ResultsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ResultsViewModel(
    observeResults: ObserveResultsUseCase,
    private val removeResult: RemoveResultUseCase,
    private val clearResults: ClearResultsUseCase
) : ViewModel() {

    val uiState: StateFlow<ResultsState> = observeResults()
        .map { ResultsState(results = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultsState())

    fun onEvent(event: ResultsEvent) {
        when (event) {
            is ResultsEvent.RemoveAt -> removeResult(event.index)
            is ResultsEvent.ClearResults -> clearResults()
        }
    }
}

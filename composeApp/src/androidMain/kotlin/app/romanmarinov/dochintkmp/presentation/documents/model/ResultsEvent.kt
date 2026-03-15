package app.romanmarinov.dochintkmp.presentation.documents.model

sealed interface ResultsEvent {
    data class RemoveAt(val index: Int) : ResultsEvent
    data object ClearResults : ResultsEvent
}

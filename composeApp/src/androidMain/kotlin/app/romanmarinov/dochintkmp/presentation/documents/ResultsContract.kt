package app.romanmarinov.dochintkmp.presentation.documents

import app.romanmarinov.dochintkmp.domain.model.MedicalData

data class ResultsState(
    val results: List<MedicalData> = emptyList()
)

sealed interface ResultsEvent {
    data class RemoveAt(val index: Int) : ResultsEvent
    data object ClearResults : ResultsEvent
}

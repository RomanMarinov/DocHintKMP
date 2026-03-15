package app.romanmarinov.dochintkmp.presentation.documents.model

import app.romanmarinov.dochintkmp.domain.model.MedicalData

data class ResultsState(
    val results: List<MedicalData> = emptyList()
)

package app.romanmarinov.dochintkmp.presentation.documents_screen.model

import app.romanmarinov.dochintkmp.domain.model.MedicalData

data class ResultsState(
    val results: List<MedicalData> = emptyList()
)

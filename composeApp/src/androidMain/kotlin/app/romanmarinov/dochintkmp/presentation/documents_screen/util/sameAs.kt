package app.romanmarinov.dochintkmp.presentation.documents_screen.util

import app.romanmarinov.dochintkmp.domain.model.MedicalData

internal fun MedicalData.sameAs(other: MedicalData): Boolean {
    return documentType == other.documentType &&
            institution == other.institution &&
            doctorName == other.doctorName &&
            analysisDate == other.analysisDate &&
            indicators == other.indicators
}
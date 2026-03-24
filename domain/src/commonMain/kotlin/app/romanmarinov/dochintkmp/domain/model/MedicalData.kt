package app.romanmarinov.dochintkmp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalData(
    val documentType: String? = null,
    val institution: String? = null,
    val doctorName: String? = null,
    val analysisDate: String? = null,
    val indicators: List<AnalysisIndicator>? = null
)

@Serializable
data class AnalysisIndicator(
    val name: String,
    val value: String,
    val referenceRange: String? = null
)


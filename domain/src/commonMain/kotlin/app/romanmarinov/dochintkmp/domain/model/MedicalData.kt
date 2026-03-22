package app.romanmarinov.dochintkmp.domain.model

data class MedicalData(
    val documentType: String? = null,
    val institution: String? = null,
    val doctorName: String? = null,
    val analysisDate: String? = null,
    val indicators: List<AnalysisIndicator>? = null
)

data class AnalysisIndicator(
    val name: String,
    val value: String,
    val referenceRange: String? = null
)


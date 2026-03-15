package app.romanmarinov.dochintkmp.data.mapper

import app.romanmarinov.dochintkmp.data.remote.IndicatorDto
import app.romanmarinov.dochintkmp.data.remote.MedicalDataDto
import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.MedicalData

fun MedicalDataDto.toDomain(): MedicalData = MedicalData(
    documentType = documentType,
    institution = institution,
    doctorName = doctorName,
    analysisDate = analysisDate,
    indicators = indicators
        ?.filter { !it.name.isNullOrBlank() && !it.value.isNullOrBlank() }
        ?.map { it.toDomain() }
)

fun IndicatorDto.toDomain(): AnalysisIndicator = AnalysisIndicator(
    name = name.orEmpty(),
    value = value.orEmpty(),
    referenceRange = referenceRange
)

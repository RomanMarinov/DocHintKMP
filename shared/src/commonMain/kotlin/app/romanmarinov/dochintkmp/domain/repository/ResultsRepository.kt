package app.romanmarinov.dochintkmp.domain.repository

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import kotlinx.coroutines.flow.StateFlow

interface ResultsRepository {
    val results: StateFlow<List<MedicalData>>
    fun isTextAlreadyProcessed(cleanText: String): Boolean
    fun addResult(data: MedicalData)
    fun removeAt(index: Int)
    fun clearResults()
}

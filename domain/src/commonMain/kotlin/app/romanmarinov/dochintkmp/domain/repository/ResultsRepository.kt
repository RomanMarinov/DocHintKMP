package app.romanmarinov.dochintkmp.domain.repository

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import kotlinx.coroutines.flow.StateFlow

interface ResultsRepository {
    val results: StateFlow<List<HousingPaymentDocument>>
    fun isTextAlreadyProcessed(cleanText: String): Boolean
    fun markTextAsProcessed(cleanText: String)
    fun addResult(data: HousingPaymentDocument, source: String = SOURCE_UNKNOWN, processedText: String? = null)
    fun removeAt(index: Int)
    fun clearResults()

    companion object {
        const val SOURCE_AI = "ai"
        const val SOURCE_OFFLINE = "offline"
        const val SOURCE_SHARED = "shared"
        const val SOURCE_UNKNOWN = "unknown"
    }
}

package app.romanmarinov.dochintkmp.data.repository

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ResultsRepositoryImpl : ResultsRepository {

    private val _results = MutableStateFlow<List<MedicalData>>(emptyList())
    override val results: StateFlow<List<MedicalData>> = _results.asStateFlow()

    private val processedTextHashes = mutableSetOf<Int>()

    override fun isTextAlreadyProcessed(cleanText: String): Boolean {
        val normalized = cleanText
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^а-яa-z0-9.,]"), "")
        return !processedTextHashes.add(normalized.hashCode())
    }

    override fun addResult(data: MedicalData) {
        _results.update { it + data }
    }

    override fun removeAt(index: Int) {
        _results.update { list ->
            list.filterIndexed { i, _ -> i != index }
        }
    }

    override fun clearResults() {
        _results.value = emptyList()
        processedTextHashes.clear()
    }
}

package app.romanmarinov.dochintkmp.data.repository

import app.romanmarinov.dochintkmp.data.local.DatabaseDriverFactory
import app.romanmarinov.dochintkmp.data.local.ProcessedHashStorage
import app.romanmarinov.dochintkmp.data.util.currentTimeMillis
import app.romanmarinov.dochintkmp.persistence.DocHintDatabase
import app.romanmarinov.dochintkmp.persistence.MedicalResult
import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResultsRepositoryImpl(
    driverFactory: DatabaseDriverFactory,
    private val processedHashStorage: ProcessedHashStorage
) : ResultsRepository {

    private val driver = driverFactory.createDriver()
    private val database = DocHintDatabase(driver)
    private val medicalResultQueries = database.medicalResultQueries
    private val processedHashMutex = Mutex()

    private val _results = MutableStateFlow<List<MedicalData>>(loadAll())
    override val results: StateFlow<List<MedicalData>> = _results.asStateFlow()

    override fun isTextAlreadyProcessed(cleanText: String): Boolean {
        val hash = normalizeForHash(cleanText)
        return runBlocking {
            processedHashMutex.withLock {
                processedHashStorage.load().contains(hash)
            }
        }
    }

    override fun markTextAsProcessed(cleanText: String) {
        val hash = normalizeForHash(cleanText)
        runBlocking {
            processedHashMutex.withLock {
                val hashes = processedHashStorage.load()
                processedHashStorage.save(hashes + hash)
            }
        }
    }

    override fun addResult(data: MedicalData, source: String, processedText: String?) {
        val indicatorsJson = Json.encodeToString(data.indicators ?: emptyList())
        val fingerprint = processedText?.let { normalizeForHash(it) }
        medicalResultQueries.insertResult(
            documentType = data.documentType,
            institution = data.institution,
            doctorName = data.doctorName,
            analysisDate = data.analysisDate,
            indicators = indicatorsJson,
            source = source,
            createdAt = currentTimeMillis(),
            textFingerprint = fingerprint
        )
        processedText?.let { markTextAsProcessed(it) }
        _results.value = loadAll()
    }

    override fun removeAt(index: Int) {
        val ids = medicalResultQueries.selectIdsOrdered().executeAsList()
        if (index !in ids.indices) return
        val id = ids[index]
        val row = medicalResultQueries.selectById(id).executeAsList().firstOrNull()
        row?.textFingerprint?.let { fp ->
            runBlocking {
                processedHashMutex.withLock {
                    val hashes = processedHashStorage.load()
                    processedHashStorage.save(hashes - fp)
                }
            }
        }
        medicalResultQueries.deleteById(id)
        _results.update { loadAll() }
    }

    override fun clearResults() {
        medicalResultQueries.deleteAll()
        runBlocking {
            processedHashMutex.withLock {
                processedHashStorage.save(emptySet())
            }
        }
        _results.value = emptyList()
    }

    private fun loadAll(): List<MedicalData> {
        return medicalResultQueries.selectAll().executeAsList().map { it.toMedicalData() }
    }

    private fun MedicalResult.toMedicalData(): MedicalData {
        val indicatorsList = try {
            Json.decodeFromString<List<AnalysisIndicator>>(indicators)
        } catch (_: Exception) {
            emptyList<AnalysisIndicator>()
        }
        return MedicalData(
            documentType = documentType,
            institution = institution,
            doctorName = doctorName,
            analysisDate = analysisDate,
            indicators = indicatorsList.ifEmpty { null },
            source = source
        )
    }

    private fun normalizeForHash(cleanText: String): Long {
        val normalized = cleanText
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^а-яa-z0-9.,]"), "")
        return normalized.hashCode().toLong()
    }
}

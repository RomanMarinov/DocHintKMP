package app.romanmarinov.dochintkmp.data.repository

import app.romanmarinov.dochintkmp.data.local.DatabaseDriverFactory
import app.romanmarinov.dochintkmp.data.local.ProcessedHashStorage
import app.romanmarinov.dochintkmp.data.util.currentTimeMillis
import app.romanmarinov.dochintkmp.persistence.DocHintDatabase
import app.romanmarinov.dochintkmp.persistence.HousingResult
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val driver = driverFactory.createDriver()
    private val database = DocHintDatabase(driver)
    private val housingResultQueries = database.housingResultQueries
    private val processedHashMutex = Mutex()

    private val _results = MutableStateFlow<List<HousingPaymentDocument>>(loadAll())
    override val results: StateFlow<List<HousingPaymentDocument>> = _results.asStateFlow()

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

    override fun addResult(data: HousingPaymentDocument, source: String, processedText: String?) {
        val payloadJson = json.encodeToString(data)
        val fingerprint = processedText?.let { normalizeForHash(it) }
        housingResultQueries.insertResult(
            documentType = data.documentType,
            documentDate = data.documentDate,
            source = source,
            createdAt = currentTimeMillis(),
            textFingerprint = fingerprint,
            payload = payloadJson
        )
        processedText?.let { markTextAsProcessed(it) }
        _results.value = loadAll()
    }

    override fun removeAt(index: Int) {
        val ids = housingResultQueries.selectIdsOrdered().executeAsList()
        if (index !in ids.indices) return
        val id = ids[index]
        val row = housingResultQueries.selectById(id).executeAsList().firstOrNull()
        row?.textFingerprint?.let { fp ->
            runBlocking {
                processedHashMutex.withLock {
                    val hashes = processedHashStorage.load()
                    processedHashStorage.save(hashes - fp)
                }
            }
        }
        housingResultQueries.deleteById(id)
        _results.update { loadAll() }
    }

    override fun clearResults() {
        housingResultQueries.deleteAll()
        runBlocking {
            processedHashMutex.withLock {
                processedHashStorage.save(emptySet())
            }
        }
        _results.value = emptyList()
    }

    private fun loadAll(): List<HousingPaymentDocument> {
        return housingResultQueries.selectAll().executeAsList().map { it.toHousingPaymentDocument() }
    }

    private fun HousingResult.toHousingPaymentDocument(): HousingPaymentDocument {
        return json.decodeFromString(payload)
    }

    private fun normalizeForHash(cleanText: String): Long {
        val normalized = cleanText
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^а-яa-z0-9.,]"), "")
        return normalized.hashCode().toLong()
    }
}

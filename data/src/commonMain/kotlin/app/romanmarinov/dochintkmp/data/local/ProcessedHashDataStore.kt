package app.romanmarinov.dochintkmp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

private const val PROCESSED_HASHES_FILE = "processed_hashes.preferences_pb"
private val HASHES_KEY = stringPreferencesKey("hashes")

/**
 * Creates a DataStore instance for processed hashes using platform-specific path.
 * Uses PreferenceDataStoreFactory.createWithPath for KMP compatibility.
 */
fun createProcessedHashDataStore(
    producePath: () -> String,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { producePath().toPath() },
    corruptionHandler = null,
    scope = scope,
    migrations = emptyList()
)

/**
 * Creates ProcessedHashStorage backed by DataStore Preferences (shared Android + iOS).
 */
fun createProcessedHashStorage(producePath: () -> String): ProcessedHashStorage {
    val dataStore = createProcessedHashDataStore(producePath)
    return ProcessedHashStorageDataStore(dataStore)
}

fun getProcessedHashesFileName(): String = PROCESSED_HASHES_FILE

private class ProcessedHashStorageDataStore(
    private val dataStore: DataStore<Preferences>
) : ProcessedHashStorage {

    override suspend fun load(): Set<Long> {
        return dataStore.data.map { prefs ->
            prefs[HASHES_KEY]?.let { json ->
                try {
                    Json.decodeFromString<List<Long>>(json).toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } ?: emptySet()
        }.first()
    }

    override suspend fun save(hashes: Set<Long>) {
        dataStore.edit { prefs ->
            prefs[HASHES_KEY] = Json.encodeToString(hashes.toList())
        }
    }
}

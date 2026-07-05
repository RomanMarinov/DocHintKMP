package app.romanmarinov.dochintkmp.data.local

/**
 * Storage for processed text hashes (duplicate detection).
 * Uses DataStore Preferences (KMP) on both Android and iOS via createWithPath.
 */
interface ProcessedHashStorage {
    suspend fun load(): Set<Long>
    suspend fun save(hashes: Set<Long>)
}

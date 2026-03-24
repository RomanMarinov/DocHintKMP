package app.romanmarinov.dochintkmp.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache of processed hashes with async persistence to DataStore/file.
 * Exposes sync API for repository compatibility.
 */
class ProcessedHashCache(
    private val storage: ProcessedHashStorage,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val mutex = Mutex()
    private var hashes: Set<Long> = runBlocking(Dispatchers.Default) { storage.load() }

    fun contains(hash: Long): Boolean = hash in hashes

    fun add(hash: Long) {
        runBlocking {
            mutex.withLock {
                hashes = hashes + hash
                storage.save(hashes)
            }
        }
    }

    fun clear() {
        scope.launch {
            mutex.withLock {
                hashes = emptySet()
                storage.save(emptySet())
            }
        }
    }
}

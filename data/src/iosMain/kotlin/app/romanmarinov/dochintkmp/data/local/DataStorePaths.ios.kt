@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package app.romanmarinov.dochintkmp.data.local

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Returns the full path for the processed hashes DataStore file on iOS.
 */
fun getProcessedHashesDataStorePath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(directory).path + "/" + getProcessedHashesFileName()
}

package app.romanmarinov.dochintkmp.util

/**
 * Платформенный логгер для KMP. На Android — android.util.Log, на iOS — NSLog/OSLog.
 */
expect fun logDebug(tag: String, message: String)

expect fun logError(tag: String, message: String, throwable: Throwable? = null)

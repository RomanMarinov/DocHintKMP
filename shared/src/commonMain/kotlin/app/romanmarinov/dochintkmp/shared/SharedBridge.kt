package app.romanmarinov.dochintkmp.shared

import app.romanmarinov.dochintkmp.domain.model.FileType

/**
 * Minimal bridge to ensure shared module compiles with domain/data dependencies.
 * Used by iOS when building Shared.framework.
 */
object SharedBridge {
    fun getSupportedFileTypes(): List<FileType> = FileType.entries
}

package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen

import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ExtractTextFromImageUseCase
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ParseWithLlmUseCase

/**
 * "Port" for AI scanner domain interactions.
 * Helps Android ViewModel unit-tests by allowing fakes instead of heavy Android use-case graphs.
 */
interface AiScannerPort {
    suspend fun extractTextOffline(uri: Uri, fileType: FileType): String
    suspend fun extractTextFromImage(fileRef: String): String
    suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult
    fun isDuplicate(cleanText: String): Boolean
    fun addResult(data: MedicalData, processedText: String)
}

class AiScannerPortImpl(
    private val extractTextFromImageUseCase: ExtractTextFromImageUseCase,
    private val extractTextOfflineUseCase: ExtractTextOfflineUseCase,
    private val parseWithLlmUseCase: ParseWithLlmUseCase,
    private val checkDuplicateUseCase: CheckDuplicateUseCase,
    private val addResultUseCase: AddResultUseCase,
) : AiScannerPort {

    override suspend fun extractTextOffline(uri: Uri, fileType: FileType): String =
        extractTextOfflineUseCase(uri, fileType)

    override suspend fun extractTextFromImage(fileRef: String): String =
        extractTextFromImageUseCase(fileRef)

    override suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult =
        parseWithLlmUseCase(apiKey, cleanText)

    override fun isDuplicate(cleanText: String): Boolean = checkDuplicateUseCase(cleanText)

    override fun addResult(data: MedicalData, processedText: String) {
        addResultUseCase(data, processedText, ResultsRepository.SOURCE_AI)
    }
}


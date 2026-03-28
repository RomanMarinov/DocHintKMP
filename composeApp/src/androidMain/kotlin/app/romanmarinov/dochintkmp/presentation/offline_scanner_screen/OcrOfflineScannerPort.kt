package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen

import android.net.Uri
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.data.parser.RuleParser

/**
 * "Port" for offline scanner interactions.
 *
 * Keeps [OcrOfflineScannerViewModel] unit-testable by allowing fakes instead of heavy use-case graphs.
 */
interface OcrOfflineScannerPort {
    suspend fun extractTextOffline(uri: Uri, fileType: FileType): String
    fun isDuplicate(cleanText: String): Boolean
    fun parse(cleanText: String): MedicalData
    fun addResult(data: MedicalData, processedText: String)
}

class OcrOfflineScannerPortImpl(
    private val extractTextOffline: ExtractTextOfflineUseCase,
    private val checkDuplicate: CheckDuplicateUseCase,
    private val ruleParser: RuleParser,
    private val addResult: AddResultUseCase,
) : OcrOfflineScannerPort {

    override suspend fun extractTextOffline(uri: Uri, fileType: FileType): String =
        extractTextOffline(uri, fileType)

    override fun isDuplicate(cleanText: String): Boolean = checkDuplicate(cleanText)

    override fun parse(cleanText: String): MedicalData = ruleParser.parse(cleanText)

    override fun addResult(data: MedicalData, processedText: String) {
        addResult(data, processedText, ResultsRepository.SOURCE_OFFLINE)
    }
}


package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen

import android.net.Uri
import app.romanmarinov.dochintkmp.data.parser.HousingBillParser
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase

/**
 * "Port" for offline scanner interactions.
 *
 * Keeps [OcrOfflineScannerViewModel] unit-testable by allowing fakes instead of heavy use-case graphs.
 */
interface OcrOfflineScannerPort {
    suspend fun extractTextOffline(uri: Uri, fileType: FileType): String
    fun isDuplicate(cleanText: String): Boolean
    fun parse(cleanText: String): HousingPaymentDocument
    fun addResult(data: HousingPaymentDocument, processedText: String)
}

class OcrOfflineScannerPortImpl(
    private val extractTextOfflineUseCase: ExtractTextOfflineUseCase,
    private val checkDuplicate: CheckDuplicateUseCase,
    private val housingBillParser: HousingBillParser,
    private val addResultUseCase: AddResultUseCase,
) : OcrOfflineScannerPort {

    override suspend fun extractTextOffline(uri: Uri, fileType: FileType): String =
        extractTextOfflineUseCase(uri, fileType)

    override fun isDuplicate(cleanText: String): Boolean = checkDuplicate(cleanText)

    override fun parse(cleanText: String): HousingPaymentDocument = housingBillParser.parse(cleanText)

    override fun addResult(data: HousingPaymentDocument, processedText: String) {
        addResultUseCase(data, processedText, ResultsRepository.SOURCE_OFFLINE)
    }
}

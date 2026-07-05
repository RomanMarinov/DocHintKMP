package app.romanmarinov.dochintkmp.shared.ios

import app.romanmarinov.dochintkmp.data.parser.HousingBillParser
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * KMP controller for Offline Scanner screen.
 * iOS extracts text natively (Vision, PDFKit); this controller handles parsing and saving.
 */
class OfflineScannerController : KoinComponent {

    private val housingBillParser: HousingBillParser by inject()
    private val checkDuplicateUseCase: CheckDuplicateUseCase by inject()
    private val addResultUseCase: AddResultUseCase by inject()

    /**
     * Process raw extracted text: clean, pre-filter, check duplicate, parse.
     * @return [ProcessResult] with data or errorType (one of them non-null).
     */
    fun processText(rawText: String): ProcessResult {
        if (rawText.isBlank()) {
            return ProcessResult(errorType = ErrorType.EMPTY_TEXT)
        }

        val cleanText = TextCleaner.cleanHousing(rawText)

        when (val preFilter = TextCleaner.housingPreFilter(cleanText)) {
            is TextCleaner.HousingPreFilterResult.TooShort ->
                return ProcessResult(errorType = ErrorType.TOO_SHORT)
            is TextCleaner.HousingPreFilterResult.NoHousingBill ->
                return ProcessResult(errorType = ErrorType.NO_HOUSING_BILL)
            is TextCleaner.HousingPreFilterResult.Ok -> { /* continue */ }
        }

        if (checkDuplicateUseCase(cleanText)) {
            return ProcessResult(errorType = ErrorType.DUPLICATE_DOCUMENT)
        }

        return try {
            val data = housingBillParser.parse(cleanText)
            ProcessResult(data = data, processedText = cleanText)
        } catch (e: Exception) {
            ProcessResult(errorType = ErrorType.PARSE_FAILED)
        }
    }

    fun saveDocument(data: HousingPaymentDocument, processedText: String) {
        addResultUseCase(data, processedText, ResultsRepository.SOURCE_OFFLINE)
    }

    data class ProcessResult(
        val data: HousingPaymentDocument? = null,
        val processedText: String? = null,
        val errorType: ErrorType? = null
    )

    enum class ErrorType {
        EMPTY_TEXT,
        TOO_SHORT,
        NO_HOUSING_BILL,
        DUPLICATE_DOCUMENT,
        PARSE_FAILED
    }
}

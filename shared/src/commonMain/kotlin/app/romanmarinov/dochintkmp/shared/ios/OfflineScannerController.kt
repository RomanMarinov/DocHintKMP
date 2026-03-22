package app.romanmarinov.dochintkmp.shared.ios

import app.romanmarinov.dochintkmp.data.parser.RuleParser
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * KMP controller for Offline Scanner screen.
 * iOS extracts text natively (Vision, PDFKit); this controller handles parsing and saving.
 */
class OfflineScannerController : KoinComponent {

    private val ruleParser: RuleParser by inject()
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

        val cleanText = TextCleaner.clean(rawText)

        when (val preFilter = TextCleaner.preFilter(cleanText)) {
            is TextCleaner.PreFilterResult.TooShort ->
                return ProcessResult(errorType = ErrorType.TOO_SHORT)
            is TextCleaner.PreFilterResult.NoMedicalData ->
                return ProcessResult(errorType = ErrorType.NO_MEDICAL_DATA)
            is TextCleaner.PreFilterResult.Ok -> { /* continue */ }
        }

        if (checkDuplicateUseCase(cleanText)) {
            return ProcessResult(errorType = ErrorType.DUPLICATE_DOCUMENT)
        }

        return try {
            val data = ruleParser.parse(cleanText)
            ProcessResult(data = data)
        } catch (e: Exception) {
            ProcessResult(errorType = ErrorType.PARSE_FAILED)
        }
    }

    /**
     * Save parsed medical data to documents list.
     */
    fun saveDocument(data: MedicalData) {
        addResultUseCase(data)
    }

    data class ProcessResult(
        val data: MedicalData? = null,
        val errorType: ErrorType? = null
    )

    enum class ErrorType {
        EMPTY_TEXT,
        TOO_SHORT,
        NO_MEDICAL_DATA,
        DUPLICATE_DOCUMENT,
        PARSE_FAILED
    }
}

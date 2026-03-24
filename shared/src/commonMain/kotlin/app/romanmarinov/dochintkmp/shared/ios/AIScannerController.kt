package app.romanmarinov.dochintkmp.shared.ios

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * KMP controller for AI Scanner screen.
 * iOS extracts text via Vision, calls OpenRouter from Swift; this controller handles duplicate check and saving.
 */
class AIScannerController : KoinComponent {

    private val checkDuplicateUseCase: CheckDuplicateUseCase by inject()
    private val addResultUseCase: AddResultUseCase by inject()

    fun checkDuplicate(cleanText: String): Boolean = checkDuplicateUseCase(cleanText)

    fun saveDocument(data: MedicalData, processedText: String) {
        addResultUseCase(data, processedText, ResultsRepository.SOURCE_AI)
    }
}

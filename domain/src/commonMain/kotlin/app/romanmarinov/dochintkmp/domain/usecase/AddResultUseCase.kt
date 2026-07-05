package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository

class AddResultUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke(data: HousingPaymentDocument, processedText: String? = null, source: String = ResultsRepository.SOURCE_UNKNOWN) {
        repository.addResult(data, source, processedText)
    }
}

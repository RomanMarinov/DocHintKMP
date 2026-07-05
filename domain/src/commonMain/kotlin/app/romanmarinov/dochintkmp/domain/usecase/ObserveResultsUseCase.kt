package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveResultsUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke(): StateFlow<List<HousingPaymentDocument>> {
        return repository.results
    }
}

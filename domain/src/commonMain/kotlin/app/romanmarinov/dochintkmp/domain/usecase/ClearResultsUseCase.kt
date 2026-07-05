package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository

class ClearResultsUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke() {
        repository.clearResults()
    }
}


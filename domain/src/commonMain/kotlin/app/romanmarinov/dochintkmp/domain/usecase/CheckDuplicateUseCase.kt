package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository

class CheckDuplicateUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke(cleanText: String): Boolean {
        return repository.isTextAlreadyProcessed(cleanText)
    }
}


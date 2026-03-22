package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository

class RemoveResultUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke(index: Int) {
        repository.removeAt(index)
    }
}


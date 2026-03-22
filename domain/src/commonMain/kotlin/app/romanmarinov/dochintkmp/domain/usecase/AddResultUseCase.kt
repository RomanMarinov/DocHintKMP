package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository

class AddResultUseCase(
    private val repository: ResultsRepository
) {
    operator fun invoke(data: MedicalData) {
        repository.addResult(data)
    }
}


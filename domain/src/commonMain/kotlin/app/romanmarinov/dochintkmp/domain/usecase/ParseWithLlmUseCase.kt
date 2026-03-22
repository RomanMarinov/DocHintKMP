package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.MedicalRepository

class ParseWithLlmUseCase(
    private val repository: MedicalRepository
) {
    suspend operator fun invoke(apiKey: String, cleanText: String): ParseResult {
        return repository.parseWithLlm(apiKey, cleanText)
    }
}


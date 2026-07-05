package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.DocumentParseRepository

class ParseWithLlmUseCase(
    private val repository: DocumentParseRepository
) {
    suspend operator fun invoke(apiKey: String, cleanText: String): ParseResult {
        return repository.parseWithLlm(apiKey, cleanText)
    }
}

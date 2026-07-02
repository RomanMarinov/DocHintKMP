package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.repository.DocumentParseRepository

/**
 * Извлечение текста из изображения/документа через OCR.
 * [fileRef] — платформенная ссылка: на Android URI.toString(), на iOS путь/URL.
 */
class ExtractTextFromImageUseCase(
    private val repository: DocumentParseRepository
) {
    suspend operator fun invoke(fileRef: String, forLlm: Boolean = true): String {
        return repository.extractText(fileRef, forLlm)
    }
}

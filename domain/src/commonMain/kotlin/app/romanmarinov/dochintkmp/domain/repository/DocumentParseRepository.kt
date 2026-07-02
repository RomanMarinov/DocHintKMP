package app.romanmarinov.dochintkmp.domain.repository

import app.romanmarinov.dochintkmp.domain.model.ParseResult

/**
 * Репозиторий для извлечения текста и парсинга через LLM.
 * [fileRef] — платформенная ссылка на файл: на Android URI.toString(), на iOS — путь или URL.
 */
interface DocumentParseRepository {
    suspend fun extractText(fileRef: String, forLlm: Boolean = true): String
    suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult
}

package app.romanmarinov.dochintkmp.data.repository

import android.net.Uri
import app.romanmarinov.dochintkmp.data.ocr.OcrEngine
import app.romanmarinov.dochintkmp.data.remote.OpenRouterClient
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.DocumentParseRepository

/**
 * Android-реализация DocumentParseRepository:
 * extractText — через Android OCR (Uri из fileRef), parseWithLlm — через общий OpenRouterClient.
 */
class DocumentParseRepositoryImpl(
    private val ocrEngine: OcrEngine,
    private val openRouterClient: OpenRouterClient
) : DocumentParseRepository {

    override suspend fun extractText(fileRef: String, forLlm: Boolean): String {
        val uri = Uri.parse(fileRef)
        val rawText = ocrEngine.recognizeText(uri)
        if (rawText.isBlank()) {
            throw IllegalStateException("OCR не распознал текст в изображении")
        }

        val cleanText = TextCleaner.cleanHousing(rawText)

        when (TextCleaner.housingPreFilter(cleanText)) {
            is TextCleaner.HousingPreFilterResult.TooShort ->
                throw IllegalStateException(
                    "Слишком мало текста. Убедитесь, что на фото — квитанция ЖКХ."
                )
            is TextCleaner.HousingPreFilterResult.NoHousingBill ->
                throw IllegalStateException(
                    "Документ не содержит данных квитанции ЖКХ." +
                    if (forLlm) " Запрос к LLM не отправлен." else ""
                )
            is TextCleaner.HousingPreFilterResult.Ok -> { }
        }

        return cleanText
    }

    override suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult {
        return openRouterClient.parseWithLlm(apiKey, cleanText)
    }
}

package app.romanmarinov.dochintkmp.data.repository

import android.net.Uri
import app.romanmarinov.dochintkmp.data.ocr.OcrEngine
import app.romanmarinov.dochintkmp.data.remote.OpenRouterClient
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.MedicalRepository

/**
 * Android-реализация MedicalRepository:
 * extractText — через Android OCR (Uri из fileRef), parseWithLlm — через общий OpenRouterClient.
 */
class MedicalRepositoryImpl(
    private val ocrEngine: OcrEngine,
    private val openRouterClient: OpenRouterClient
) : MedicalRepository {

    override suspend fun extractText(fileRef: String, forLlm: Boolean): String {
        val uri = Uri.parse(fileRef)
        val rawText = ocrEngine.recognizeText(uri)
        if (rawText.isBlank()) {
            throw IllegalStateException("OCR не распознал текст в изображении")
        }

        val cleanText = TextCleaner.clean(rawText)

        when (TextCleaner.preFilter(cleanText)) {
            is TextCleaner.PreFilterResult.TooShort ->
                throw IllegalStateException(
                    "Слишком мало текста. Убедитесь, что на фото — медицинский документ."
                )
            is TextCleaner.PreFilterResult.NoMedicalData ->
                throw IllegalStateException(
                    "Документ не содержит медицинских данных " +
                    "(анализов крови, дат, показателей)." +
                    if (forLlm) " Запрос к LLM не отправлен." else ""
                )
            is TextCleaner.PreFilterResult.Ok -> { }
        }

        return cleanText
    }

    override suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult {
        return openRouterClient.parseWithLlm(apiKey, cleanText)
    }
}

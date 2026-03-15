package app.romanmarinov.dochintkmp.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import app.romanmarinov.dochintkmp.data.ocr.DocxTextExtractor
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
import app.romanmarinov.dochintkmp.data.ocr.paddle.PaddleOcrEngine
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-специфичный use case: офлайн извлечение текста из PDF/DOCX/изображений.
 * Для iOS будет отдельная реализация (или общий интерфейс с actual).
 */
class ExtractTextOfflineUseCase(
    private val ocrEngine: PaddleOcrEngine,
    private val pdfPageRenderer: PdfPageRenderer,
    private val docxTextExtractor: DocxTextExtractor,
    private val contentResolver: ContentResolver
) {
    suspend operator fun invoke(uri: Uri, fileType: FileType): String {
        val rawText = when (fileType) {
            FileType.IMAGE, FileType.PNG -> ocrEngine.recognizeText(uri)

            FileType.PDF -> {
                val bitmaps = pdfPageRenderer.renderPages(uri)
                try {
                    bitmaps.map { ocrEngine.recognizeBitmap(it) }
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                } finally {
                    bitmaps.forEach { it.recycle() }
                }
            }

            FileType.TXT -> withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw IllegalStateException("Не удалось прочитать TXT файл")
            }

            FileType.DOCX -> docxTextExtractor.extractText(uri)
        }

        if (rawText.isBlank()) {
            throw IllegalStateException("Не удалось извлечь текст из документа")
        }

        val cleanText = TextCleaner.clean(rawText)

        when (TextCleaner.preFilter(cleanText)) {
            is TextCleaner.PreFilterResult.TooShort ->
                throw IllegalStateException(
                    "Слишком мало текста. Убедитесь, что документ — медицинский."
                )
            is TextCleaner.PreFilterResult.NoMedicalData ->
                throw IllegalStateException(
                    "Документ не содержит медицинских данных (анализов крови, дат, показателей)."
                )
            is TextCleaner.PreFilterResult.Ok -> { }
        }

        return cleanText
    }
}

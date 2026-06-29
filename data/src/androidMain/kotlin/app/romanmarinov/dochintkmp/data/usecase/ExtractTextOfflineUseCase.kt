package app.romanmarinov.dochintkmp.data.usecase

import android.content.ContentResolver
import android.net.Uri
import app.romanmarinov.dochintkmp.data.ocr.DocxTextExtractor
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
import app.romanmarinov.dochintkmp.data.ocr.PdfTextExtractor
import app.romanmarinov.dochintkmp.data.ocr.paddle.PaddleOcrEngine
import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-специфичный use case: офлайн извлечение текста из PDF/DOCX/изображений.
 * Держим в `:data`, потому что он использует Android-инфраструктуру (`Uri`, `ContentResolver`, Android OCR).
 */
class ExtractTextOfflineUseCase(
    private val ocrEngine: PaddleOcrEngine,
    private val pdfPageRenderer: PdfPageRenderer,
    private val pdfTextExtractor: PdfTextExtractor,
    private val docxTextExtractor: DocxTextExtractor,
    private val contentResolver: ContentResolver
) {
    suspend operator fun invoke(uri: Uri, fileType: FileType): String {
        val rawText = when (fileType) {
            FileType.IMAGE, FileType.PNG -> ocrEngine.recognizeText(uri)

            FileType.PDF -> extractPdfText(uri)

            FileType.TXT -> withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw IllegalStateException("Не удалось прочитать TXT файл")
            }

            FileType.DOCX -> docxTextExtractor.extractText(uri)
        }

        if (rawText.isBlank()) {
            throw IllegalStateException("Не удалось извлечь текст из документа")
        }

        val cleanText = TextCleaner.cleanHousing(rawText)

        when (TextCleaner.housingPreFilter(cleanText)) {
            is TextCleaner.HousingPreFilterResult.TooShort ->
                throw IllegalStateException("Слишком мало текста. Убедитесь, что это квитанция ЖКУ.")
            is TextCleaner.HousingPreFilterResult.NoHousingBill ->
                throw IllegalStateException("Документ не похож на квитанцию ЖКУ (платёжный документ за коммуналку).")
            is TextCleaner.HousingPreFilterResult.Ok -> Unit
        }

        return cleanText
    }

    private suspend fun extractPdfText(uri: Uri): String {
        val nativeText = pdfTextExtractor.extractText(uri)
        val ocrText = runCatching {
            val bitmaps = pdfPageRenderer.renderPages(uri)
            try {
                bitmaps.map { ocrEngine.recognizeBitmap(it) }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            } finally {
                bitmaps.forEach { it.recycle() }
            }
        }.getOrDefault("")

        return selectBestPdfText(nativeText, ocrText)
    }

    private fun selectBestPdfText(nativeText: String, ocrText: String): String {
        val candidates = listOf(
            nativeText to "native",
            ocrText to "ocr",
            listOf(nativeText, ocrText).filter { it.isNotBlank() }.joinToString("\n") to "merged"
        ).filter { it.first.isNotBlank() }

        return candidates.maxByOrNull { housingTextScore(it.first) }?.first.orEmpty()
    }

    private fun housingTextScore(text: String): Int {
        val clean = TextCleaner.cleanHousing(text)
        val passes = TextCleaner.housingPreFilter(clean) is TextCleaner.HousingPreFilterResult.Ok
        return clean.length + if (passes) 10_000 else 0
    }
}

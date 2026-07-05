package app.romanmarinov.dochintkmp.data.ocr

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Извлекает текстовый слой из PDF (без OCR).
 * Для квитанций ЖКУ с встроенным текстом это намного точнее, чем Paddle OCR.
 */
class PdfTextExtractor(private val context: Context) {

    init {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { document ->
                    PDFTextStripper().getText(document)
                }
            }.orEmpty()
        }.getOrDefault("")
    }

    private companion object {
        @Volatile
        private var initialized = false
    }
}

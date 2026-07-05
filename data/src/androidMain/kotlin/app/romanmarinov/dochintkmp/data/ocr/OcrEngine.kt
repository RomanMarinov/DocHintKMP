package app.romanmarinov.dochintkmp.data.ocr

import android.net.Uri

/**
 * Android-специфичный OCR движок (принимает Uri).
 * Для iOS будет отдельная реализация с путём/URL.
 */
interface OcrEngine {
    suspend fun recognizeText(uri: Uri): String
    fun release() {}
}


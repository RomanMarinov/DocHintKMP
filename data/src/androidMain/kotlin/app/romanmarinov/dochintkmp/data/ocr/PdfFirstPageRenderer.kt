package app.romanmarinov.dochintkmp.data.ocr

import android.graphics.Bitmap
import android.net.Uri

/**
 * Minimal abstraction for rendering the first page of a PDF.
 * Used by AI scanner ViewModel and also by unit tests (via fakes).
 */
interface PdfFirstPageRenderer {
    suspend fun renderFirstPageToBitmap(uri: Uri): Bitmap?
}


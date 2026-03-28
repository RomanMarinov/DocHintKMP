package app.romanmarinov.dochintkmp.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfPageRenderer(private val context: Context) : PdfFirstPageRenderer {

    override suspend fun renderFirstPageToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            fd.use { descriptor ->
                PdfRenderer(descriptor).use { pdf ->
                    if (pdf.pageCount == 0) return@withContext null
                    val page = pdf.openPage(0)
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun renderPages(uri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Не удалось открыть PDF файл")

        fd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            renderer.use { pdf ->
                (0 until pdf.pageCount).map { i ->
                    val page = pdf.openPage(i)
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap
                }
            }
        }
    }
}


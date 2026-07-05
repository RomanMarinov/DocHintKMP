package app.romanmarinov.dochintkmp.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.io.File

class TesseractOcrEngine(private val context: Context) : OcrEngine {

    private var tessApi: TessBaseAPI? = null

    override suspend fun recognizeText(uri: Uri): String = withContext(Dispatchers.IO) {
        val api = getOrInitApi()
        val bitmap = loadBitmap(uri)
        try {
            api.setImage(bitmap)
            buildTableFromBoundingBoxes(api)
        } finally {
            bitmap.recycle()
        }
    }

    private fun buildTableFromBoundingBoxes(api: TessBaseAPI): String {
        val iter = api.resultIterator ?: return api.utF8Text ?: ""
        val words = mutableListOf<Pair<String, Rect>>()
        try {
            while (iter.next(PageIteratorLevel.RIL_WORD)) {
                val rect = iter.getBoundingRect(PageIteratorLevel.RIL_WORD)
                val text = iter.getUTF8Text(PageIteratorLevel.RIL_WORD)?.takeIf { it.isNotBlank() }
                if (text != null && rect != null) {
                    words.add(text to rect)
                }
            }
        } finally {
            iter.delete()
        }

        if (words.isEmpty()) return api.utF8Text ?: ""

        val tolerance = 22
        val rows = mutableMapOf<Int, MutableList<Pair<String, Rect>>>()
        for ((text, rect) in words.sortedBy { it.second.top }) {
            val top = rect.top
            val existing = rows.entries.find { abs(it.key - top) <= tolerance }
            if (existing != null) {
                existing.value.add(text to rect)
            } else {
                rows[top] = mutableListOf(text to rect)
            }
        }

        return rows.entries
            .sortedBy { it.key }
            .map { (_, cells) ->
                cells.sortedBy { it.second.left }
                    .joinToString(" ") { it.first.trim() }
                    .trim()
            }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun getOrInitApi(): TessBaseAPI {
        tessApi?.let { return it }
        val dataPath = File(context.filesDir, "tesseract")
        val tessDataDir = File(dataPath, "tessdata")
        if (!tessDataDir.exists()) tessDataDir.mkdirs()
        copyAssetIfNeeded(tessDataDir, "eng.traineddata")
        copyAssetIfNeeded(tessDataDir, "rus.traineddata")

        val api = TessBaseAPI()
        if (!api.init(dataPath.absolutePath, "rus+eng")) {
            api.recycle()
            throw IllegalStateException(
                "Не удалось инициализировать Tesseract. Проверьте файлы traineddata."
            )
        }
        tessApi = api
        return api
    }

    private fun copyAssetIfNeeded(tessDataDir: File, filename: String) {
        val outFile = File(tessDataDir, filename)
        if (outFile.exists()) return
        try {
            context.assets.open("tessdata/$filename").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Отсутствует $filename в assets/tessdata/. " +
                "Скачайте с https://github.com/tesseract-ocr/tessdata/tree/4.0.0"
            )
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalStateException("Не удалось прочитать изображение")
    }

    override fun release() {
        tessApi?.recycle()
        tessApi = null
    }
}


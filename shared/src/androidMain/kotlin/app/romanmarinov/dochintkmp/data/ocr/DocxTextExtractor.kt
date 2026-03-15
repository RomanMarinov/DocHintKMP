package app.romanmarinov.dochintkmp.data.ocr

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.zip.ZipInputStream

class DocxTextExtractor(private val context: Context) {

    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        val xmlBytes = extractDocumentXml(uri)
            ?: throw IllegalStateException("Не удалось прочитать DOCX: word/document.xml не найден")
        parseDocumentXml(xmlBytes)
    }

    private fun extractDocumentXml(uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть DOCX файл")
        inputStream.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        return zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    private fun parseDocumentXml(xmlBytes: ByteArray): String {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xmlBytes.inputStream(), "UTF-8")

        val result = StringBuilder()
        var inTableCell = false
        var inParagraph = false
        var cellIndex = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> inParagraph = true
                        "tc" -> {
                            inTableCell = true
                            if (cellIndex > 0) result.append('\t')
                        }
                        "tr" -> cellIndex = 0
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (text != null && text.isNotBlank()) result.append(text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            if (inParagraph && !inTableCell) result.append('\n')
                            inParagraph = false
                        }
                        "tc" -> {
                            inTableCell = false
                            cellIndex++
                        }
                        "tr" -> result.append('\n')
                    }
                }
            }
            eventType = parser.next()
        }
        return result.toString().trim()
    }
}

package app.romanmarinov.dochintkmp.data.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextCleanerTest {

    @Test
    fun clean_removesJunkLines_andNormalizesSpacing() {
        val raw = """
            Лицензия 12345
            Общий анализ крови
            
            Гемоглобин     120
            Телефон: +7(999)000-00-00
            Лейкоциты   6.2
        """.trimIndent()

        val cleaned = TextCleaner.clean(raw)

        assertTrue(cleaned.contains("Общий анализ крови"))
        assertTrue(cleaned.contains("Гемоглобин 120"))
        assertTrue(cleaned.contains("Лейкоциты 6.2"))
        assertTrue(!cleaned.contains("Лицензия"))
        assertTrue(!cleaned.contains("Телефон"))
    }

    @Test
    fun clean_limitsOutputByMaxLength() {
        val long = buildString {
            repeat(1000) { append("Гемоглобин 120 ") }
        }

        val cleaned = TextCleaner.clean(long, maxLength = 100)

        assertEquals(100, cleaned.length)
    }

    @Test
    fun preFilter_returnsTooShort_forVerySmallText() {
        val result = TextCleaner.preFilter("кровь")

        assertEquals(TextCleaner.PreFilterResult.TooShort, result)
    }

    @Test
    fun preFilter_returnsOk_forOakKeywords() {
        val result = TextCleaner.preFilter("Общий анализ крови: гемоглобин 120")

        assertEquals(TextCleaner.PreFilterResult.Ok, result)
    }

    @Test
    fun preFilter_returnsNoMedicalData_whenNoMedicalSignals() {
        val result = TextCleaner.preFilter("Привет мир, обычный текст без медицинских терминов")

        assertEquals(TextCleaner.PreFilterResult.NoMedicalData, result)
    }

    @Test
    fun preFilter_returnsOk_forGeneralMedicalAndDate() {
        val result = TextCleaner.preFilter("Результат лабораторного исследования от 12/03/2025")

        assertEquals(TextCleaner.PreFilterResult.Ok, result)
    }
}

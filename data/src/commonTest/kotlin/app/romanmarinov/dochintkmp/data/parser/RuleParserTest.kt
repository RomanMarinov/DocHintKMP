package app.romanmarinov.dochintkmp.data.parser

import app.romanmarinov.dochintkmp.domain.model.LaboratoryIndicatorNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuleParserTest {

    private val parser = RuleParser()

    @Test
    fun parse_detectsOak_andExtractsCoreFieldsAndIndicators() {
        val text = """
            Общий анализ крови
            Дата анализа: 12.03.2025
            Врач: Иванов И.И.
            ООО "ИНВИТРО"
            Гемоглобин 120
            Лейкоциты 6.1
            Тромбоциты 250
            Лимфоциты % 35
            СОЭ 8
            MCV 90
            RDW-CV 13.2
        """.trimIndent()

        val result = parser.parse(text)

        assertEquals("ОАК", result.documentType)
        assertEquals("12.03.2025", result.analysisDate)
        assertNotNull(result.doctorName)
        assertTrue((result.institution ?: "").contains("ИНВИТРО", ignoreCase = true))
        val names = result.indicators.orEmpty().map { it.name }.toSet()
        assertTrue(names.contains(LaboratoryIndicatorNames.HEMOGLOBIN))
        assertTrue(names.contains(LaboratoryIndicatorNames.LEUKOCYTES))
        assertTrue(names.contains(LaboratoryIndicatorNames.PLATELETS))
        assertTrue(names.contains(LaboratoryIndicatorNames.LYMPHOCYTES_PERCENT))
        assertTrue(names.contains(LaboratoryIndicatorNames.SOE))
    }

    @Test
    fun parse_detectsBak_andExtractsBakIndicators() {
        val text = """
            Биохимический анализ крови
            11/03/2025
            Глюкоза 5.4
            Холестерин 4.8
            ЛПНП 2.1
            ЛПВП 1.3
            Креатинин 89
            Альбумин 42
        """.trimIndent()

        val result = parser.parse(text)

        assertTrue(result.documentType == "БАК" || result.documentType == null)
        assertEquals("11/03/2025", result.analysisDate)
        val names = result.indicators.orEmpty().map { it.name }.toSet()
        assertTrue(names.contains(LaboratoryIndicatorNames.GLUCOSE))
        assertTrue(names.contains(LaboratoryIndicatorNames.CHOLESTEROL))
        assertTrue(names.contains(LaboratoryIndicatorNames.CREATININE))
        assertTrue(names.contains(LaboratoryIndicatorNames.ALBUMIN))
    }

    @Test
    fun parse_throwsUnsupported_forClearlyUnsupportedAnalysis() {
        val text = """
            Общий анализ мочи
            Моча
            Копрология
            Онкомаркеры
        """.trimIndent()

        val ex = assertFailsWith<IllegalStateException> { parser.parse(text) }
        assertTrue(ex.message.orEmpty().contains("Тип анализа не поддерживается"))
    }

    @Test
    fun parse_throwsWhenNoIndicatorsFound() {
        val text = """
            Общий анализ крови
            Дата анализа: 12.03.2025
            Врач: Петров П.П.
        """.trimIndent()

        val ex = assertFailsWith<IllegalStateException> { parser.parse(text) }
        assertTrue(ex.message.orEmpty().contains("Не удалось извлечь показатели"))
    }

    @Test
    fun parse_extractsDateInShortFormat() {
        val text = """
            Биохимия
            Дата исследования: 01.02.25
            Глюкоза 5.0
            Креатинин 80
        """.trimIndent()

        val result = parser.parse(text)

        assertEquals("01.02.25", result.analysisDate)
    }
}

package app.romanmarinov.dochintkmp.data.text

import kotlin.test.Test
import kotlin.test.assertTrue

class TextCleanerHousingTest {

    @Test
    fun housingPreFilter_acceptsRealBillSnippet() {
        val text = """
            ПЛАТЕЖНЫЙ ДОКУМЕНТ № 202602_60ЕХ209700-10 ПО Л/С 0667000001
            за ФЕВРАЛЬ - 2026 (расчетный период)
            Сумма к оплате за расчетный период 3 625.82 рублей
        """.trimIndent()

        val result = TextCleaner.housingPreFilter(TextCleaner.cleanHousing(text))
        assertTrue(result is TextCleaner.HousingPreFilterResult.Ok)
    }

    @Test
    fun housingPreFilter_acceptsTextWithYoLetter() {
        val text = """
            Платёжный документ по л/с 0667000001
            расчётный период февраль 2026
            коммунальные услуги
        """.trimIndent()

        val result = TextCleaner.housingPreFilter(TextCleaner.cleanHousing(text))
        assertTrue(result is TextCleaner.HousingPreFilterResult.Ok)
    }

    @Test
    fun housingPreFilter_acceptsPersonalAccountPatternOnly() {
        val text = "ПО Л/С 0667000001 ИЖКУ: 60ЕХ209700-10"
        val result = TextCleaner.housingPreFilter(TextCleaner.cleanHousing(text))
        assertTrue(result is TextCleaner.HousingPreFilterResult.Ok)
    }
}

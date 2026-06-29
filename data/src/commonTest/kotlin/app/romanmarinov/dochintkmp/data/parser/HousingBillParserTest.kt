package app.romanmarinov.dochintkmp.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HousingBillParserTest {

    private val parser = HousingBillParser()

    @Test
    fun parse_mainBill_extractsCoreFieldsAndServices() {
        val text = """
            ПЛАТЕЖНЫЙ ДОКУМЕНТ № 202602_60ЕХ209700-10 ПО Л/С 0667000001
            Идентификатор платежного документа: 60ЕХ209700-10-6021
            за ФЕВРАЛЬ - 2026 (расчетный период)
            Адрес жилого помещения: Вологодская обл, г. Вологда, ул. Северная, д. 10б, кв. 1
            Площадь жилого помещения: 63.3/31.2 кв.м.
            Количество проживающих: 1
            ООО "УК "ФЛАГМАН" ИНН 3525307492
            Сумма к оплате за расчетный период 3 625.82 рублей
            Оплачено денежных средств 2 648.96 рублей
            Дата последней поступившей оплаты 20.02.2026
            Содержание помещения м[2*] общ. пл - - 27.588152 1 746.33 - - - - 1 746.33 1 667.13 3 413.46
            Холодная вода м[2*] - 63.3 (1) 0.147 9.31 - - - - 9.31 46.82 56.13
            Подогрев воды Гкал 13 (2) - 140.73 1 829.49 - - - - 1 829.49 981.83 2 811.32
            Видеодомофон кварт 1 - 50 50.00 - - - - 50.00 0.00 50.00
        """.trimIndent()

        val result = parser.parse(text)

        assertEquals("Квитанция ЖКУ", result.documentType)
        assertEquals("2026-02", result.documentDate)
        assertEquals("0667000001", result.personalAccountNumber)
        assertEquals("3625.82", result.amountDueForPeriod)
        assertNotNull(result.institution)
        assertTrue(result.institution!!.contains("ФЛАГМАН", ignoreCase = true))
        val names = result.serviceLines.orEmpty().map { it.name }.toSet()
        assertTrue(names.contains("Содержание помещения"))
        assertTrue(names.contains("Подогрев воды"))
        assertTrue(names.contains("Видеодомофон"))
    }

    @Test
    fun parse_capitalRepairBill_detectsCategory() {
        val text = """
            ПЛАТЕЖНЫЙ ДОКУМЕНТ № 202511_60ЕХ209700-11КР ПО Л/С 0667000001
            Идентификатор платежного документа: 60ЕХ209700-11-5111
            за НОЯБРЬ - 2025 (расчетный период)
            ООО "УК "ФЛАГМАН"
            Сумма к оплате за расчетный период 867.21 рублей
            Взнос на капитальный ремонт м[2*] общ. пл - - 13.7 867.21 - - - - 867.21 867.21 1 734.42
        """.trimIndent()

        val result = parser.parse(text)

        assertEquals("2025-11", result.documentDate)
        assertEquals("867.21", result.amountDueForPeriod)
        assertEquals("Взнос на капитальный ремонт", result.serviceLines?.single()?.name)
    }

    @Test
    fun parse_throwsWhenNotHousingBill() {
        val text = """
            Общий анализ крови
            Гемоглобин 120
        """.trimIndent()

        val ex = assertFailsWith<IllegalStateException> { parser.parse(text) }
        assertTrue(ex.message.orEmpty().contains("квитанц"))
    }

    @Test
    fun parse_throwsWhenNoServiceLines() {
        val text = """
            ПЛАТЕЖНЫЙ ДОКУМЕНТ № 202602_60ЕХ209700-10 ПО Л/С 0667000001
            за ФЕВРАЛЬ - 2026 (расчетный период)
            Сумма к оплате за расчетный период 100.00 рублей
        """.trimIndent()

        val ex = assertFailsWith<IllegalStateException> { parser.parse(text) }
        assertTrue(ex.message.orEmpty().contains("услуг"))
    }
}

package app.romanmarinov.dochintkmp.data.mapper

import app.romanmarinov.dochintkmp.domain.model.HousingBillCategory
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.HousingServiceLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HousingBillMapperTest {

    @Test
    fun toMedicalData_andBack_preservesCoreFields() {
        val original = HousingPaymentDocument(
            documentType = "Квитанция ЖКУ",
            institution = "ООО УК ФЛАГМАН",
            documentDate = "2026-02",
            category = HousingBillCategory.MAIN,
            personalAccountNumber = "0667000001",
            paymentDocumentId = "60ЕХ209700-10-6021",
            propertyAddress = "ул. Северная, 10б, кв. 1",
            amountDueForPeriod = "3625.82",
            serviceLines = listOf(
                HousingServiceLine(name = "Подогрев воды", amountToPay = "1829.49", tariff = "140.73")
            )
        )

        val medical = original.toMedicalData()
        val restored = medical.toHousingPaymentDocumentOrNull()

        assertNotNull(restored)
        assertEquals("2026-02", restored.documentDate)
        assertEquals("0667000001", restored.personalAccountNumber)
        assertEquals("3625.82", restored.amountDueForPeriod)
        assertEquals("Подогрев воды", restored.serviceLines?.first()?.name)
    }
}

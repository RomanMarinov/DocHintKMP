package app.romanmarinov.dochintkmp.data.mapper

import app.romanmarinov.dochintkmp.data.remote.IndicatorDto
import app.romanmarinov.dochintkmp.data.remote.MedicalDataDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedicalDataMapperTest {

    @Test
    fun toDomain_mapsAllFields_andFiltersInvalidIndicators() {
        val dto = MedicalDataDto(
            documentType = "ОАК",
            institution = "Инвитро",
            doctorName = "Иванов И.И.",
            analysisDate = "12.03.2025",
            indicators = listOf(
                IndicatorDto(name = "Гемоглобин", value = "120", referenceRange = "120-160"),
                IndicatorDto(name = "Лейкоциты", value = "6.2", referenceRange = null),
                IndicatorDto(name = "", value = "10"),
                IndicatorDto(name = "Тромбоциты", value = null),
            )
        )

        val domain = dto.toDomain()

        assertEquals("ОАК", domain.documentType)
        assertEquals("Инвитро", domain.institution)
        assertEquals("Иванов И.И.", domain.doctorName)
        assertEquals("12.03.2025", domain.analysisDate)
        assertEquals(2, domain.indicators?.size)
        assertEquals("Гемоглобин", domain.indicators?.get(0)?.name)
        assertEquals("120", domain.indicators?.get(0)?.value)
        assertEquals("120-160", domain.indicators?.get(0)?.referenceRange)
    }

    @Test
    fun toDomain_keepsIndicatorsNull_whenSourceNull() {
        val dto = MedicalDataDto(indicators = null)

        val domain = dto.toDomain()

        assertNull(domain.indicators)
    }

    @Test
    fun indicatorToDomain_usesEmptyStrings_forNullNameAndValue() {
        val dto = IndicatorDto(name = null, value = null, referenceRange = "1-2")

        val domain = dto.toDomain()

        assertEquals("", domain.name)
        assertEquals("", domain.value)
        assertEquals("1-2", domain.referenceRange)
    }
}

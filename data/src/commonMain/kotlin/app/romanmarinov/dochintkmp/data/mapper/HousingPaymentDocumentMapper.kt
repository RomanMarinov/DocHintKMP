package app.romanmarinov.dochintkmp.data.mapper

import app.romanmarinov.dochintkmp.data.remote.HousingPaymentDocumentDto
import app.romanmarinov.dochintkmp.data.remote.HousingServiceLineDto
import app.romanmarinov.dochintkmp.domain.model.HousingBillCategory
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.HousingServiceGroup
import app.romanmarinov.dochintkmp.domain.model.HousingServiceLine
import app.romanmarinov.dochintkmp.domain.model.HousingVolumeBasis

fun HousingPaymentDocumentDto.toDomain(): HousingPaymentDocument {
    val lines = serviceLines
        ?.filter { !it.name.isNullOrBlank() && !it.amountToPay.isNullOrBlank() }
        ?.map { it.toDomain() }
        ?.takeIf { it.isNotEmpty() }

    return HousingPaymentDocument(
        documentType = documentType ?: "Квитанция ЖКУ",
        institution = institution,
        documentDate = documentDate,
        source = source,
        category = parseCategory(category, documentType),
        documentNumber = documentNumber,
        paymentDocumentId = paymentDocumentId,
        personalAccountNumber = personalAccountNumber,
        unifiedPersonalAccount = unifiedPersonalAccount,
        housingUtilitiesId = housingUtilitiesId,
        propertyAddress = propertyAddress,
        payerName = payerName,
        totalAreaSqm = totalAreaSqm,
        livingAreaSqm = livingAreaSqm,
        residentsCount = residentsCount,
        amountDueForPeriod = amountDueForPeriod,
        amountPaid = amountPaid,
        lastPaymentDate = lastPaymentDate,
        debtFromPreviousPeriods = debtFromPreviousPeriods,
        serviceLines = lines
    )
}

fun HousingServiceLineDto.toDomain(): HousingServiceLine = HousingServiceLine(
    name = name.orEmpty(),
    group = parseServiceGroup(group),
    unit = unit,
    volume = volume,
    volumeBasis = parseVolumeBasis(volumeBasis),
    tariff = tariff,
    amountToPay = amountToPay.orEmpty()
)

private fun parseCategory(raw: String?, documentType: String?): HousingBillCategory {
    val combined = ((raw ?: "") + " " + (documentType ?: "")).lowercase()
    return when {
        combined.contains("капремонт") || combined.contains("капитальный") ->
            HousingBillCategory.CAPITAL_REPAIR
        else -> HousingBillCategory.MAIN
    }
}

private fun parseServiceGroup(raw: String?): HousingServiceGroup {
    val key = raw?.trim()?.uppercase()?.replace(' ', '_') ?: return HousingServiceGroup.UTILITIES
    return HousingServiceGroup.entries.firstOrNull { it.name == key } ?: HousingServiceGroup.UTILITIES
}

private fun parseVolumeBasis(raw: String?): HousingVolumeBasis? {
    val key = raw?.trim()?.uppercase()?.replace(' ', '_') ?: return null
    return HousingVolumeBasis.entries.firstOrNull { it.name == key }
}

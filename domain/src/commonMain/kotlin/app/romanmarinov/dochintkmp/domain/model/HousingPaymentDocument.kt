package app.romanmarinov.dochintkmp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class HousingBillCategory {
    MAIN,
    CAPITAL_REPAIR
}

@Serializable
enum class HousingServiceGroup {
    MAINTENANCE,
    COMMON_PROPERTY_ODN,
    UTILITIES,
    ADDITIONAL,
    CAPITAL_REPAIR
}

@Serializable
enum class HousingVolumeBasis {
    NORM,
    METER,
    OTHER
}

@Serializable
data class HousingPaymentDocument(
    val documentType: String = "Квитанция ЖКУ",
    val institution: String? = null,
    val documentDate: String? = null,
    val source: String? = null,

    val category: HousingBillCategory = HousingBillCategory.MAIN,
    val documentNumber: String? = null,
    val paymentDocumentId: String? = null,
    val personalAccountNumber: String? = null,
    val unifiedPersonalAccount: String? = null,
    val housingUtilitiesId: String? = null,

    val propertyAddress: String? = null,
    val payerName: String? = null,
    val totalAreaSqm: String? = null,
    val livingAreaSqm: String? = null,
    val residentsCount: String? = null,

    val amountDueForPeriod: String? = null,
    val amountPaid: String? = null,
    val lastPaymentDate: String? = null,
    val debtFromPreviousPeriods: String? = null,

    val serviceLines: List<HousingServiceLine>? = null
)

@Serializable
data class HousingServiceLine(
    val name: String,
    val group: HousingServiceGroup = HousingServiceGroup.UTILITIES,
    val unit: String? = null,
    val volume: String? = null,
    val volumeBasis: HousingVolumeBasis? = null,
    val tariff: String? = null,
    val amountToPay: String
)

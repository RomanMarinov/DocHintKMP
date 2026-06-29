package app.romanmarinov.dochintkmp.data.mapper

import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.HousingBillCategory
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.HousingServiceLine
import app.romanmarinov.dochintkmp.domain.model.MedicalData

const val HOUSING_META_PREFIX = "__meta__:"

fun AnalysisIndicator.displayLabel(): String =
    if (name.startsWith(HOUSING_META_PREFIX)) name.removePrefix(HOUSING_META_PREFIX) else name

fun HousingPaymentDocument.toMedicalData(): MedicalData {
    val metaIndicators = buildList {
        personalAccountNumber?.let { add(meta("Лицевой счёт", it)) }
        paymentDocumentId?.let { add(meta("ID платёжного документа", it)) }
        housingUtilitiesId?.let { add(meta("ИЖКУ", it)) }
        propertyAddress?.let { add(meta("Адрес", it)) }
        totalAreaSqm?.let { add(meta("Площадь общая", "$it м²")) }
        livingAreaSqm?.let { add(meta("Площадь жилая", "$it м²")) }
        residentsCount?.let { add(meta("Проживающих", it)) }
        amountDueForPeriod?.let { add(meta("К оплате за период", "$it ₽")) }
        amountPaid?.let { add(meta("Оплачено", "$it ₽")) }
        lastPaymentDate?.let { add(meta("Дата оплаты", it)) }
    }

    val serviceIndicators = serviceLines.orEmpty().map { line ->
        AnalysisIndicator(
            name = line.name,
            value = "${line.amountToPay} ₽",
            referenceRange = line.referenceLabel()
        )
    }

    val categoryLabel = when (category) {
        HousingBillCategory.MAIN -> "основная"
        HousingBillCategory.CAPITAL_REPAIR -> "капремонт"
    }

    return MedicalData(
        documentType = "$documentType ($categoryLabel)",
        institution = institution,
        doctorName = payerName,
        analysisDate = documentDate,
        indicators = metaIndicators + serviceIndicators,
        source = source
    )
}

fun MedicalData.toHousingPaymentDocumentOrNull(): HousingPaymentDocument? {
    if (!isHousingBillDocument()) return null

    val meta = indicators.orEmpty()
        .filter { it.name.startsWith(HOUSING_META_PREFIX) }
        .associate { it.name.removePrefix(HOUSING_META_PREFIX) to it.value }

    val serviceLines = indicators.orEmpty()
        .filterNot { it.name.startsWith(HOUSING_META_PREFIX) }
        .mapNotNull { indicator ->
            val amount = indicator.value.removeSuffix(" ₽").trim()
            if (amount.isBlank()) return@mapNotNull null
            HousingServiceLine(
                name = indicator.name,
                amountToPay = amount,
                tariff = indicator.referenceRange
            )
        }

    val category = when {
        documentType?.contains("капремонт", ignoreCase = true) == true ->
            HousingBillCategory.CAPITAL_REPAIR
        else -> HousingBillCategory.MAIN
    }

    return HousingPaymentDocument(
        documentType = documentType?.substringBefore(" (") ?: "Квитанция ЖКУ",
        institution = institution,
        documentDate = analysisDate,
        source = source,
        category = category,
        payerName = doctorName,
        personalAccountNumber = meta["Лицевой счёт"],
        paymentDocumentId = meta["ID платёжного документа"],
        housingUtilitiesId = meta["ИЖКУ"],
        propertyAddress = meta["Адрес"],
        totalAreaSqm = meta["Площадь общая"]?.removeSuffix(" м²"),
        livingAreaSqm = meta["Площадь жилая"]?.removeSuffix(" м²"),
        residentsCount = meta["Проживающих"],
        amountDueForPeriod = meta["К оплате за период"]?.removeSuffix(" ₽"),
        amountPaid = meta["Оплачено"]?.removeSuffix(" ₽"),
        lastPaymentDate = meta["Дата оплаты"],
        serviceLines = serviceLines.takeIf { it.isNotEmpty() }
    )
}

fun MedicalData.isHousingBillDocument(): Boolean {
    val type = documentType.orEmpty()
    return type.contains("жку", ignoreCase = true) ||
        type.contains("жкх", ignoreCase = true) ||
        type.contains("квитанц", ignoreCase = true) ||
        indicators.orEmpty().any { it.name.startsWith(HOUSING_META_PREFIX) }
}

private fun meta(label: String, value: String): AnalysisIndicator =
    AnalysisIndicator(name = "$HOUSING_META_PREFIX$label", value = value)

private fun HousingServiceLine.referenceLabel(): String? {
    val parts = buildList {
        volume?.let { add("$it ${unit.orEmpty()}".trim()) }
        tariff?.let { add("тариф $it") }
        volumeBasis?.let { add(it.name.lowercase()) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
}

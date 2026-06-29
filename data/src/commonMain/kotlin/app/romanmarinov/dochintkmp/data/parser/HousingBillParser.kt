package app.romanmarinov.dochintkmp.data.parser

import app.romanmarinov.dochintkmp.data.text.TextCleaner
import app.romanmarinov.dochintkmp.domain.model.HousingBillCategory
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.HousingServiceGroup
import app.romanmarinov.dochintkmp.domain.model.HousingServiceLine
import app.romanmarinov.dochintkmp.domain.model.HousingVolumeBasis

/**
 * Rule-based parser для квитанций ЖКУ (платёжные документы УК).
 */
class HousingBillParser {

    fun parse(cleanText: String): HousingPaymentDocument {
        val text = cleanText.replace('\u00A0', ' ')
        if (!TextCleaner.isHousingBillText(text)) {
            throw IllegalStateException(
                "Документ не похож на квитанцию ЖКУ. Прикрепите платёжный документ за коммунальные услуги."
            )
        }

        val lower = TextCleaner.normalizeForHousingMatch(text)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val category = detectCategory(lower, text)
        val billingPeriod = extractBillingPeriod(text)
        val serviceLines = extractServiceLines(lines)
        if (serviceLines.isEmpty()) {
            throw IllegalStateException(
                "Не удалось извлечь услуги из квитанции. Попробуйте более чёткий PDF или AI Сканер."
            )
        }

        return HousingPaymentDocument(
            documentType = "Квитанция ЖКУ",
            institution = extractInstitution(text),
            documentDate = billingPeriod,
            category = category,
            documentNumber = extractDocumentNumber(text),
            paymentDocumentId = extractPaymentDocumentId(text),
            personalAccountNumber = extractPersonalAccount(text),
            unifiedPersonalAccount = extractUnifiedAccount(text),
            housingUtilitiesId = extractHousingUtilitiesId(text),
            propertyAddress = extractAddress(text),
            payerName = extractPayerName(lines),
            totalAreaSqm = extractArea(text, isLiving = false),
            livingAreaSqm = extractArea(text, isLiving = true),
            residentsCount = extractResidents(text),
            amountDueForPeriod = extractAmount(text, "Сумма к оплате за расчетный период"),
            amountPaid = extractAmount(text, "Оплачено денежных средств"),
            lastPaymentDate = extractLastPaymentDate(text),
            debtFromPreviousPeriods = extractAmount(text, "Итого к оплате с учетом задолженности"),
            serviceLines = serviceLines
        )
    }

    private fun detectCategory(lower: String, text: String): HousingBillCategory {
        val isCapitalOnly = lower.contains("капитальный ремонт") &&
            !lower.contains("содержание помещения") &&
            !lower.contains("подогрев воды")
        if (isCapitalOnly) return HousingBillCategory.CAPITAL_REPAIR
        if (text.contains("60ЕХ209700-11") || lower.contains("-11кр")) {
            return HousingBillCategory.CAPITAL_REPAIR
        }
        return HousingBillCategory.MAIN
    }

    private fun extractBillingPeriod(text: String): String? {
        MONTH_PERIOD.find(text)?.let { match ->
            val monthName = match.groupValues[1]
            val year = match.groupValues[2]
            val month = MONTHS[monthName.lowercase()] ?: return@let null
            return "${year.toInt()}-${month.toString().padStart(2, '0')}"
        }
        GENERIC_PERIOD.find(text)?.let { return it.groupValues[1] }
        return null
    }

    private fun extractDocumentNumber(text: String): String? =
        Regex("""ПЛАТЕЖНЫЙ ДОКУМЕНТ\s*№\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractPaymentDocumentId(text: String): String? =
        Regex("""Идентификатор платежного документа:\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractPersonalAccount(text: String): String? {
        Regex("""ПО Л/С\s*(\d+)""", RegexOption.IGNORE_CASE).find(text)?.let {
            return it.groupValues[1]
        }
        return Regex("""лицевой счет:\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)
    }

    private fun extractUnifiedAccount(text: String): String? =
        Regex("""Единый лицевой счет:\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractHousingUtilitiesId(text: String): String? =
        Regex("""ИЖКУ:\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractAddress(text: String): String? =
        Regex("""Адрес жилого помещения:\s*(.+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.trim()

    private fun extractPayerName(lines: List<String>): String? {
        val idx = lines.indexOfFirst { it.contains("плательщика", ignoreCase = true) }
        if (idx < 0 || idx + 1 >= lines.size) return null
        val candidate = lines[idx + 1]
        if (candidate.contains("Адрес", ignoreCase = true)) return null
        return candidate.takeIf { it.length in 3..80 }
    }

    private fun extractArea(text: String, isLiving: Boolean): String? {
        val match = Regex(
            """Площадь жилого помещения:\s*([\d.,]+)/([\d.,]+)""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        return if (isLiving) match.groupValues[2] else match.groupValues[1]
    }

    private fun extractResidents(text: String): String? =
        Regex("""Количество проживающих:\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractAmount(text: String, label: String): String? {
        val pattern = Regex(
            """${Regex.escape(label)}\s*([\d\s]+[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        return pattern.find(text)?.groupValues?.get(1)?.replace(" ", "")?.replace(',', '.')
    }

    private fun extractLastPaymentDate(text: String): String? =
        Regex("""Дата последней поступившей оплаты\s*(\d{1,2}\.\d{1,2}\.\d{4})""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)

    private fun extractInstitution(text: String): String? {
        UK_PATTERN.find(text)?.let { return normalizeOrg(it.value) }
        val lines = text.lines().map { it.trim() }
        return lines.firstOrNull { line ->
            line.contains("УПРАВЛЯЮЩАЯ КОМПАНИЯ", ignoreCase = true) ||
                line.contains("УК ", ignoreCase = true) ||
                line.contains("""ООО "УК""", ignoreCase = true)
        }?.let { normalizeOrg(it) }
    }

    private fun normalizeOrg(raw: String): String {
        return raw
            .replace(Regex("""ИНН\s*\d+.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""КПП\s*\d+.*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(120)
    }

    private fun extractServiceLines(lines: List<String>): List<HousingServiceLine> {
        val results = mutableListOf<HousingServiceLine>()
        for (line in lines) {
            val definition = SERVICE_DEFINITIONS.firstOrNull { def ->
                line.contains(def.name, ignoreCase = true)
            } ?: continue
            val parsed = parseServiceLine(line, definition) ?: continue
            if (results.none { it.name == parsed.name }) {
                results.add(parsed)
            }
        }
        return results
    }

    private fun parseServiceLine(line: String, definition: ServiceDefinition): HousingServiceLine? {
        val amounts = MONEY_PATTERN.findAll(line).map { it.value.replace(" ", "").replace(',', '.') }.toList()
        if (amounts.isEmpty()) return null

        val amountToPay = pickAmountToPay(amounts) ?: return null
        val tariff = pickTariff(line, amounts, amountToPay)
        val volume = pickVolume(line, definition)
        val volumeBasis = when {
            line.contains("(1)") -> HousingVolumeBasis.NORM
            line.contains("(2)") -> HousingVolumeBasis.METER
            line.contains("(3)") -> HousingVolumeBasis.OTHER
            else -> null
        }

        return HousingServiceLine(
            name = definition.name,
            group = definition.group,
            unit = definition.unit,
            volume = volume,
            volumeBasis = volumeBasis,
            tariff = tariff,
            amountToPay = amountToPay
        )
    }

    private fun pickAmountToPay(amounts: List<String>): String? {
        val candidates = amounts
            .mapNotNull { it.toDoubleOrNull()?.let { value -> value to it } }
            .filter { (value, _) -> value in 1.0..100_000.0 }
        if (candidates.isEmpty()) return null

        val grouped = candidates.groupBy { it.first }
        val repeated = grouped.entries
            .filter { it.value.size >= 2 }
            .maxByOrNull { it.key }
        if (repeated != null) return repeated.value.first().second

        return candidates
            .filter { (value, _) -> value >= 3.0 }
            .maxByOrNull { it.first }
            ?.second
            ?: candidates.last().second
    }

    private fun pickTariff(line: String, amounts: List<String>, amountToPay: String): String? {
        val withoutPay = amounts.filter { it != amountToPay }
        val tariffCandidate = withoutPay
            .mapNotNull { it.toDoubleOrNull()?.let { v -> v to it } }
            .filter { (value, _) -> value in 0.01..500.0 }
            .minByOrNull { it.first }
            ?.second
        if (tariffCandidate != null) return tariffCandidate

        val afterDashes = Regex("""[-–]\s*[-–]\s*([\d.,]+)""").find(line)?.groupValues?.get(1)
        return afterDashes?.replace(',', '.')
    }

    private fun pickVolume(line: String, definition: ServiceDefinition): String? {
        if (definition.unit == "кварт") {
            return Regex("""\b(\d+)\b""").findAll(line).map { it.value }.firstOrNull { it == "1" } ?: "1"
        }
        val gcal = Regex("""Гкал\s*([\d.,]+)""", RegexOption.IGNORE_CASE).find(line)
        if (gcal != null) return gcal.groupValues[1].replace(',', '.')

        val areaVolume = Regex("""([\d.,]+)\s*\(1\)""").find(line)
        if (areaVolume != null) return areaVolume.groupValues[1].replace(',', '.')

        return null
    }

    private data class ServiceDefinition(
        val name: String,
        val group: HousingServiceGroup,
        val unit: String? = null
    )

    companion object {
        private val MONEY_PATTERN = Regex("""\d{1,3}(?: \d{3})*[.,]\d{2}|\d+[.,]\d{2}""")
        private val MONTH_PERIOD = Regex(
            """за\s+([А-Яа-яЁё]+)\s*[-–]\s*(\d{4})\s*\(расчетный период\)""",
            RegexOption.IGNORE_CASE
        )
        private val GENERIC_PERIOD = Regex("""(\d{6})_""")
        private val UK_PATTERN = Regex(
            """(?:ООО|ОАО|АО|ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ)[^.\n]{0,80}(?:УПРАВЛЯЮЩАЯ КОМПАНИЯ|УК)[^.\n]{0,40}""",
            RegexOption.IGNORE_CASE
        )

        private val MONTHS = mapOf(
            "январь" to 1, "февраль" to 2, "март" to 3, "апрель" to 4,
            "май" to 5, "июнь" to 6, "июль" to 7, "август" to 8,
            "сентябрь" to 9, "октябрь" to 10, "ноябрь" to 11, "декабрь" to 12
        )

        private val SERVICE_DEFINITIONS = listOf(
            ServiceDefinition("Содержание помещения", HousingServiceGroup.MAINTENANCE, "м²"),
            ServiceDefinition("Холодная вода", HousingServiceGroup.COMMON_PROPERTY_ODN, "м²"),
            ServiceDefinition("Электрическая энергия", HousingServiceGroup.COMMON_PROPERTY_ODN, "м²"),
            ServiceDefinition("Сточные воды", HousingServiceGroup.COMMON_PROPERTY_ODN, "м²"),
            ServiceDefinition("Подогрев воды", HousingServiceGroup.UTILITIES, "Гкал"),
            ServiceDefinition("Горячее водоснабжение", HousingServiceGroup.UTILITIES),
            ServiceDefinition("Отопление", HousingServiceGroup.UTILITIES, "Гкал"),
            ServiceDefinition("Видеодомофон", HousingServiceGroup.ADDITIONAL, "кварт"),
            ServiceDefinition("Взнос на капитальный ремонт", HousingServiceGroup.CAPITAL_REPAIR, "м²")
        )
    }
}

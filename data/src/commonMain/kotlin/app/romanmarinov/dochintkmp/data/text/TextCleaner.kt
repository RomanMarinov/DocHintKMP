package app.romanmarinov.dochintkmp.data.text

object TextCleaner {

    fun cleanHousing(raw: String, maxLength: Int = 8000): String {
        val lines = raw
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cleaned = lines.filterNot { isHousingJunkLine(it) }
        val result = (cleaned.ifEmpty { lines }).joinToString("\n")
        return result.normalizeDates().take(maxLength)
    }

    fun normalizeForHousingMatch(text: String): String =
        text.lowercase()
            .replace('ё', 'е')
            .replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")

    fun isHousingBillText(text: String): Boolean =
        housingPreFilter(text) is HousingPreFilterResult.Ok

    fun housingPreFilter(cleanedText: String): HousingPreFilterResult {
        if (cleanedText.length < 20) return HousingPreFilterResult.TooShort

        val lower = normalizeForHousingMatch(cleanedText)
        val score = HOUSING_KEYWORDS.count { lower.contains(it) }
        if (score >= 2) return HousingPreFilterResult.Ok
        if (score >= 1 && (lower.contains("платежн") || lower.contains("документ"))) {
            return HousingPreFilterResult.Ok
        }
        if (HOUSING_REGEX_SIGNALS.any { it.containsMatchIn(lower) }) {
            return HousingPreFilterResult.Ok
        }
        return HousingPreFilterResult.NoHousingBill
    }

    private fun isHousingJunkLine(line: String): Boolean {
        return HOUSING_JUNK_PATTERNS.any { line.contains(it, ignoreCase = true) }
    }

    private fun String.normalizeDates(): String {
        return this
            .replace(Regex("(\\d{2})\\.(\\d{2})\\.(\\d{4})"), "$1.$2.$3")
            .replace(Regex("(\\d{2})/(\\d{2})/(\\d{4})"), "$1.$2.$3")
    }

    sealed class HousingPreFilterResult {
        data object Ok : HousingPreFilterResult()
        data object TooShort : HousingPreFilterResult()
        data object NoHousingBill : HousingPreFilterResult()
    }

    private val HOUSING_KEYWORDS = listOf(
        "платежн",
        "платежный документ",
        "коммунальн",
        "жилого помещения",
        "лицевой счет",
        "лицевой счёт",
        "л/с",
        "расчетный период",
        "расчётный период",
        "содержание помещения",
        "капитальный ремонт",
        "к оплате за расчетный период",
        "сумма к оплате",
        "управляющая компания",
        "ижку",
        "жкх",
        "жку",
        "квитанц"
    )

    private val HOUSING_REGEX_SIGNALS = listOf(
        Regex("""л\s*/\s*с\s*\d"""),
        Regex("""по\s+л\s*/\s*с\s*\d"""),
        Regex("""\d{6}_\d{2}[а-яa-zё]+-\d""", RegexOption.IGNORE_CASE),
        Regex("""\d{2}[а-яa-zё]+-\d{2}-\d{4}""", RegexOption.IGNORE_CASE),
        Regex("""идентификатор платежного документа"""),
        Regex("""единый лицевой счет"""),
        Regex("""единый лицевой счёт""")
    )

    private val HOUSING_JUNK_PATTERNS = listOf(
        "кодировка текста",
        "utf-8",
        "win1251",
        "страница",
        "предельный (максимальный) индекс"
    )
}

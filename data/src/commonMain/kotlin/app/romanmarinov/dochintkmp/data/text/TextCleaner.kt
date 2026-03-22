package app.romanmarinov.dochintkmp.data.text

object TextCleaner {

    fun clean(raw: String, maxLength: Int = 2500): String {
        val lines = raw
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cleaned = lines.filterNot { isJunkLine(it) }
        val result = (cleaned.ifEmpty { lines }).joinToString("\n")

        return result.normalizeDates().take(maxLength)
    }

    fun preFilter(cleanedText: String): PreFilterResult {
        if (cleanedText.length < 20) return PreFilterResult.TooShort

        val isOak = OAK_KEYWORDS.any { cleanedText.contains(it, ignoreCase = true) }
        val isBak = BAK_KEYWORDS.any { cleanedText.contains(it, ignoreCase = true) }
        if (isOak || isBak) return PreFilterResult.Ok

        val hasGeneralMedical = GENERAL_KEYWORDS.any { cleanedText.contains(it, ignoreCase = true) }
        val hasDate = Regex("\\d{2}[./]\\d{2}[./]\\d{2,4}").containsMatchIn(cleanedText)

        if (hasGeneralMedical && hasDate) return PreFilterResult.Ok

        return PreFilterResult.NoMedicalData
    }

    private fun isJunkLine(line: String): Boolean {
        return JUNK_PATTERNS.any { line.contains(it, ignoreCase = true) }
    }

    private fun String.normalizeDates(): String {
        return this
            .replace(Regex("(\\d{2})\\.(\\d{2})\\.(\\d{4})"), "$1.$2.$3")
            .replace(Regex("(\\d{2})/(\\d{2})/(\\d{4})"), "$1.$2.$3")
    }

    private val JUNK_PATTERNS = listOf(
        "лицензия", "инн", "огрн", "кпп", "расчетн", "банк",
        "телефон:", "тел.:", "тел:", "факс", "e-mail", "www.",
        "http", "адрес:", "индекс", "copyright", "все права",
        "гост р", "исо 9001", "сертифицирован", "bureau veritas",
        "электронном экземпляре", "не являются диагнозом",
        "необходима консультация", "перейти на исходный"
    )

    val OAK_KEYWORDS = listOf(
        "общий анализ крови", "оак", "клинический анализ крови",
        "гемоглобин", "эритроцит", "лейкоцит", "тромбоцит",
        "гематокрит", "нейтрофил", "лимфоцит", "моноцит",
        "базофил", "эозинофил", "соэ", "цветовой показатель",
        "mcv", "mch", "mchc", "rdw"
    )

    val BAK_KEYWORDS = listOf(
        "биохимия", "биохимический", "бак",
        "глюкоз", "холестерин", "лпнп", "лпвп",
        "алт", "аст", "креатинин", "hba1c", "гликированн",
        "ттг", "тиреотропн", "щелочная фосфатаза",
        "лдг", "общий белок", "с-реактивн", "crp",
        "билирубин", "мочевин", "ггт", "калий", "натрий"
    )

    private val GENERAL_KEYWORDS = listOf(
        "анализ", "результат", "пациент", "дата рождения",
        "норма", "референ", "исследовани", "клинический",
        "лаборатор", "кровь", "blood", "врач", "инвитро",
        "медси", "кдл", "гемотест", "хеликс"
    )

    sealed class PreFilterResult {
        data object Ok : PreFilterResult()
        data object TooShort : PreFilterResult()
        data object NoMedicalData : PreFilterResult()
    }
}


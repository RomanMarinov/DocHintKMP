package app.romanmarinov.dochintkmp.data.parser

import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.LaboratoryIndicatorNames
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.util.logDebug
import kotlin.math.min

/**
 * Rule-based parser для медицинских анализов (ОАК/БАК).
 * Общая логика для Android и iOS.
 */
class RuleParser {

    fun parse(cleanText: String): MedicalData {
        val lines = cleanText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val fullText = lines.joinToString("\n")

        if (isUnsupportedAnalysisType(fullText)) {
            throw IllegalStateException(
                "Тип анализа не поддерживается (моча, кал, гормоны, онкомаркеры и т.п.). " +
                "Попробуйте AI Сканер для сложных документов."
            )
        }

        val docType = detectDocumentType(fullText)
        val date = extractDate(fullText)
        val doctor = extractDoctor(lines)
        val institution = extractInstitution(lines)
        val indicators = extractIndicators(lines)

        if (indicators.isEmpty()) {
            throw IllegalStateException(
                "Не удалось извлечь показатели из текста. " +
                "Попробуйте AI Сканер для сложных документов."
            )
        }

        return MedicalData(
            documentType = docType,
            institution = institution,
            doctorName = doctor,
            analysisDate = date,
            indicators = indicators
        )
    }

    private fun isUnsupportedAnalysisType(text: String): Boolean {
        val lower = text.lowercase()
        val oakScore = OAK_MARKERS.count { lower.contains(it) }
        val bakScore = BAK_MARKERS.count { lower.contains(it) }
        if (oakScore >= 1 || bakScore >= 1) return false

        val unsupportedScore = UNSUPPORTED_ANALYSIS_MARKERS.count { lower.contains(it) }
        return unsupportedScore >= 2
    }

    private fun detectDocumentType(text: String): String? {
        val lower = text.lowercase()
        val oakScore = OAK_MARKERS.count { lower.contains(it) }
        val bakScore = BAK_MARKERS.count { lower.contains(it) }

        return when {
            oakScore >= 2 || (oakScore == 1 && bakScore == 0) -> "ОАК"
            bakScore >= 2 || (bakScore == 1 && oakScore == 0) -> "БАК"
            oakScore > bakScore -> "ОАК"
            bakScore > oakScore -> "БАК"
            else -> null
        }
    }

    private fun extractDate(text: String): String? {
        val datePatterns = listOf(
            Regex("""(?:дата\s*(?:взятия|сбора|анализа|поступления|исследования)[^:]*?:\s*)(\d{1,2}[./]\d{1,2}[./]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2}[./]\d{2}[./]\d{4})"""),
            Regex("""(\d{2}[./]\d{2}[./]\d{2})(?!\d)""")
        )
        for (pattern in datePatterns) {
            pattern.find(text)?.let { return it.groupValues[1] }
        }
        return null
    }

    private fun extractDoctor(lines: List<String>): String? {
        for (line in lines) {
            val doctorMatch = DOCTOR_PREFIX.find(line) ?: continue
            val afterPrefix = line.substring(doctorMatch.range.last + 1).trim()
            val name = extractName(afterPrefix)
            if (name != null) return name
        }
        return null
    }

    private fun extractName(text: String): String? {
        val cleaned = text
            .replace(Regex("""[,;].*"""), "")
            .replace(Regex("""\d+.*"""), "")
            .trim()
        if (cleaned.length < 3) return null

        NAME_PATTERNS.forEach { p ->
            p.find(cleaned)?.let { return it.value.trim() }
        }
        return null
    }

    private fun extractInstitution(lines: List<String>): String? {
        for (line in lines) {
            INSTITUTION_PATTERNS.forEach { p ->
                p.find(line)?.let { return it.value.trim().take(80) }
            }
        }
        for (line in lines) {
            val lower = line.lowercase()
            if (!KNOWN_LABS.any { lower.contains(it) }) continue
            val labPart = extractLabNameFromLine(line)
            if (labPart != null) return labPart.take(80)
            val cleaned = line
                .replace(Regex("""^\d+\s*"""), "")
                .replace(Regex("""\d{10,}.*"""), "")
                .trim()
            if (cleaned.length in 4..80 && !cleaned.contains(Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+"""))) {
                return cleaned.take(80)
            }
        }
        return null
    }

    private fun extractLabNameFromLine(line: String): String? {
        val invitro = Regex("""(?:ООО|АО|ЗАО)?\s*["«]?ИНВИТРО[^.\n]{0,50}""", RegexOption.IGNORE_CASE).find(line)
        if (invitro != null) return invitro.value.trim()
        val medsi = Regex("""(?:АО|ООО)?\s*["«]?.*?МЕДСИ[^.\n]{0,30}""", RegexOption.IGNORE_CASE).find(line)
        if (medsi != null) return medsi.value.trim()
        return null
    }

    private fun isTableJunk(line: String): Boolean {
        val l = line.lowercase()
        return l == "нормы" || l == "отклонения" || l == "значения" ||
                l.startsWith("числовой результат") ||
                l.startsWith("выше норм") ||
                l.startsWith("ниже норм") ||
                l.startsWith("отклонение от")
    }

    private fun reconstructTableLines(lines: List<String>): List<String> {
        val cleaned = lines.map {
            it.replace(Regex("""[\[\]{}]"""), "").trim()
        }.filter { it.isNotBlank() && !isTableJunk(it) }

        val result = mutableListOf<String>()
        for (line in cleaned) {
            val first = line.firstOrNull()
            val isContinuation = first != null
                    && first.isLetter()
                    && first.isLowerCase()
                    && line.length in 4..39

            if (isContinuation && result.isNotEmpty()) {
                var merged = false
                for (lookback in 1..min(2, result.size)) {
                    val idx = result.size - lookback
                    val target = result[idx]
                    val numMatch = NUMBER_PATTERN.find(target)
                    if (numMatch != null && numMatch.range.first > 2) {
                        val namePart = target.substring(0, numMatch.range.first).trimEnd()
                        val dataPart = target.substring(numMatch.range.first)
                        result[idx] = "$namePart $line $dataPart"
                        merged = true
                        break
                    }
                }
                if (!merged) result.add(line)
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun stripNormData(line: String): String {
        return line
            .replace(RANGE_PATTERN, " ")
            .replace(REF_WITH_SIGN, " ")
            .replace(NORM_WORD, " ")
            .replace(EXP_UNIT, " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun extractIndicators(lines: List<String>): List<AnalysisIndicator> {
        val merged = reconstructTableLines(lines)
        val stripped = merged.map { stripNormData(it) }.filter { it.isNotBlank() }

        val fullText = stripped.joinToString("\n")
        val docType = detectDocumentType(fullText)
        val allowedNames = when {
            docType?.contains("ОАК", ignoreCase = true) == true ||
                docType?.contains("общий", ignoreCase = true) == true -> OAK_INDICATOR_NAMES
            docType?.contains("биохим", ignoreCase = true) == true ||
                docType?.contains("БАК", ignoreCase = true) == true -> BAK_INDICATOR_NAMES
            else -> OAK_INDICATOR_NAMES + BAK_INDICATOR_NAMES
        }
        val mapFiltered = INDICATOR_MAP.filter { (_, canonicalName) -> canonicalName in allowedNames }

        val results = mutableListOf<AnalysisIndicator>()
        val seenKeys = mutableSetOf<String>()

        for (i in stripped.indices) {
            val line = stripped[i]
            val lineWithPrev = if (i > 0) "${stripped[i - 1]} $line" else line
            val lineWithPrev2 = if (i >= 2) "${stripped[i - 2]} ${stripped[i - 1]} $line" else lineWithPrev
            val lineWithPrev3 = if (i >= 3) "${stripped[i - 3]} ${stripped[i - 2]} ${stripped[i - 1]} $line" else lineWithPrev2
            val lineWithNext = if (i + 1 < stripped.size) "$line ${stripped[i + 1]}" else line
            val lineWithNext2 = if (i + 2 < stripped.size) "$line ${stripped[i + 1]} ${stripped[i + 2]}" else lineWithNext
            val lineWithNext3 = if (i + 3 < stripped.size) "$line ${stripped[i + 1]} ${stripped[i + 2]} ${stripped[i + 3]}" else lineWithNext2
            val lineWithNext4 = if (i + 4 < stripped.size) "$line ${stripped[i + 1]} ${stripped[i + 2]} ${stripped[i + 3]} ${stripped[i + 4]}" else lineWithNext3

            for ((aliases, canonicalName) in mapFiltered) {
                if (canonicalName in seenKeys) continue
                val alias = aliases.firstOrNull { line.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithNext.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithNext2.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithNext3.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithNext4.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithPrev.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithPrev2.contains(it, ignoreCase = true) }
                    ?: aliases.firstOrNull { lineWithPrev3.contains(it, ignoreCase = true) }
                    ?: continue

                val parsed = parseIndicatorLine(line, alias)
                    ?: parseIndicatorLine(lineWithNext, alias)
                    ?: parseIndicatorLine(lineWithNext2, alias)
                    ?: parseIndicatorLine(lineWithNext3, alias)
                    ?: parseIndicatorLine(lineWithNext4, alias)
                    ?: parseIndicatorLineWithBefore(lineWithPrev, alias)
                    ?: parseIndicatorLineWithBefore(lineWithPrev2, alias)
                    ?: parseIndicatorLineWithBefore(lineWithPrev3, alias)

                if (parsed == null) continue

                if (canonicalName == LaboratoryIndicatorNames.RDW && line.lowercase().contains("rdw-sd")) {
                    continue
                }
                if (canonicalName == LaboratoryIndicatorNames.LYMPHOCYTES_PERCENT &&
                    (alias.lowercase() == "лимфоциты" || alias.lowercase() == "лимфоцит") &&
                    (line.lowercase().contains("lym#") || lineWithNext.lowercase().contains("lym#") ||
                        lineWithPrev.lowercase().contains("lym#"))
                ) continue

                logDebug(TAG, "FOUND [$canonicalName] = $parsed  (alias=\"$alias\")")
                seenKeys.add(canonicalName)
                results.add(AnalysisIndicator(name = canonicalName, value = parsed))
            }
        }

        for ((aliases, canonicalName) in mapFiltered) {
            if (canonicalName in seenKeys) continue
            val alias = aliases.firstOrNull { fullText.contains(it, ignoreCase = true) } ?: continue
            val parsed = extractFromNarrowWindow(fullText, alias) ?: continue
            logDebug(TAG, "FOUND(window) [$canonicalName] = $parsed  (alias=\"$alias\")")
            seenKeys.add(canonicalName)
            results.add(AnalysisIndicator(name = canonicalName, value = parsed))
        }

        logDebug(TAG, "Total indicators: ${results.size}")
        return results
    }

    private fun extractFromNarrowWindow(fullText: String, alias: String): String? {
        val lower = fullText.lowercase()
        var pos = 0
        while (true) {
            val idx = lower.indexOf(alias, pos)
            if (idx < 0) return null
            val windowAfter = fullText.substring(idx + alias.length).take(90)
            val numbersAfter = NUMBER_PATTERN.findAll(windowAfter).toList()
            val valueAfter = numbersAfter.firstOrNull { !isPartOfReferenceRange(windowAfter, it.range.first) }?.value
            if (valueAfter != null) return valueAfter
            val windowBefore = fullText.substring(maxOf(0, idx - 50), idx)
            val numbersBefore = NUMBER_PATTERN.findAll(windowBefore).toList()
            if (numbersBefore.isNotEmpty()) return numbersBefore.last().value
            pos = idx + 1
        }
    }

    private fun isPartOfReferenceRange(text: String, numStart: Int): Boolean {
        val before = text.substring(0, numStart).trimEnd()
        val afterStart = numStart
        if (afterStart >= text.length) return false
        val after = text.substring(afterStart).take(20)
        return before.endsWith("-") || before.endsWith("–") || before.endsWith("—") || before.endsWith("−") ||
                after.startsWith("-") || after.startsWith("–") || after.startsWith("—") || after.startsWith("−") ||
                before.endsWith("<") || before.endsWith(">") || before.endsWith("≤") || before.endsWith("≥")
    }

    private fun parseIndicatorLine(line: String, alias: String): String? {
        val idx = line.indexOf(alias, ignoreCase = true)
        if (idx < 0) return null
        val after = line.substring(idx + alias.length)
        val numbers = NUMBER_PATTERN.findAll(after).toList()
        if (numbers.isEmpty()) return null

        if (numbers.size > 1) {
            val textBeforeFirstNum = after.substring(0, numbers.first().range.first)
            if (hasOtherIndicatorBetween(textBeforeFirstNum, alias)) {
                return numbers.last().value
            }
            val resultNotInRange = numbers.firstOrNull { !isPartOfReferenceRange(after, it.range.first) }
            if (resultNotInRange != null) return resultNotInRange.value
        }
        return numbers.first().value
    }

    private fun hasOtherIndicatorBetween(text: String, currentAlias: String): Boolean {
        val lower = text.lowercase()
        val currentAliasLower = currentAlias.lowercase()
        for ((aliases, _) in INDICATOR_MAP) {
            if (aliases.any { it.lowercase() == currentAliasLower }) continue
            if (aliases.any { it.length >= 3 && lower.contains(it.lowercase()) }) return true
        }
        return false
    }

    private fun parseIndicatorLineWithBefore(lineWithPrev: String, alias: String): String? {
        val idx = lineWithPrev.indexOf(alias, ignoreCase = true)
        if (idx <= 0) return null
        val before = lineWithPrev.substring(0, idx)
        val after = lineWithPrev.substring(idx + alias.length)
        val numbersBefore = NUMBER_PATTERN.findAll(before).toList()
        if (numbersBefore.isEmpty()) {
            val numbersAfter = NUMBER_PATTERN.findAll(after).toList()
            if (numbersAfter.isEmpty()) return null
            return numbersAfter.first().value
        }
        return numbersBefore.last().value
    }

    companion object {
        private const val TAG = "RuleParser"

        private val OAK_MARKERS = listOf(
            "общий анализ крови", "оак", "клинический анализ",
            "гемоглобин", "гематокрит", "эритроцит", "лейкоцит",
            "тромбоцит", "нейтрофил", "лимфоцит", "гранулоцит"
        )

        private val BAK_MARKERS = listOf(
            "биохимия", "биохимический", "бак",
            "глюкоз", "холестерин", "алт", "аст",
            "креатинин", "билирубин", "мочевин"
        )

        private val UNSUPPORTED_ANALYSIS_MARKERS = listOf(
            "анализ мочи", "общий анализ мочи", "оам", "моча",
            "анализ кала", "копролог", "кал",
            "коагулограмм", "свёртывае",
            "гормон", "тиреотропн", "ттг", "т3", "т4",
            "онкомаркер", "пса", "ca 125", "cea",
            "иммунолог", "аллергопроба"
        )

        private val DOCTOR_PREFIX = Regex(
            """(?:врач|доктор|doctor|лечащий\s*врач|dr\.?)\s*[:.]?\s*""",
            RegexOption.IGNORE_CASE
        )

        private val NAME_PATTERNS = listOf(
            Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.?"""),
            Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+"""),
            Regex("""[А-ЯЁ][а-яё]+\s+[А-ЯЁ][а-яё]+"""),
            Regex("""[А-ЯЁ][а-яё]{2,}""")
        )

        private val KNOWN_LABS = listOf(
            "инвитро", "гемотест", "хеликс", "медси", "кдл", "лабораторн",
            "cmd", "ситилаб", "dnkom", "литех"
        )

        private val INSTITUTION_PATTERNS = listOf(
            Regex("""(?:ООО|ОАО|ЗАО|АО|ФГБУЗ|ФГБОУ|ГБУ|МУЗ|ГБУЗ)\s*[«""]?[\wА-Яа-яёЁ\s\-]+[»""]?"""),
        )

        private val NUMBER_PATTERN = Regex("""\d+[.,]?\d*""")
        private val RANGE_PATTERN = Regex("""\d+[.,]?\d*\s*[-–—−]\s*\d+[.,]?\d*""")
        private val REF_WITH_SIGN = Regex("""[<>≤≥]\s*\d+[.,]?\d*""")
        private val NORM_WORD = Regex("""(?<![а-яёА-ЯЁa-zA-Z])норм[аы](?![а-яёА-ЯЁa-zA-Z])""", RegexOption.IGNORE_CASE)
        private val EXP_UNIT = Regex("""10\^?\d+\s*/\s*[а-яА-Яa-zA-Z]+""")

        private val OAK_INDICATOR_NAMES = LaboratoryIndicatorNames.OAK_ACTIVE
        private val BAK_INDICATOR_NAMES = LaboratoryIndicatorNames.BAK_ACTIVE

        val INDICATOR_MAP: List<Pair<List<String>, String>> = listOf(
            listOf(
                "гемоглобин", "hemoglobin", "hgb", "hb", "гемоглобин общий", "(hgb) гемоглобин",
                "hgb (гемоглобин)", "гемоглобин (hgb)", "гемоглобина", "гемоглоб.", "hgb)"
            ) to LaboratoryIndicatorNames.HEMOGLOBIN,
            listOf(
                "лейкоцит", "leukocyte", "wbc", "количество лейкоцит", "(wbc) лейкоцит",
                "лейкоциты (wbc)", "лейкоциты", "wbc)"
            ) to LaboratoryIndicatorNames.LEUKOCYTES,
            listOf(
                "тромбоциты", "тромбоцит", "platelet", "plt", "количество тромбоцит",
                "(plt) тромбоцит", "тромбоциты (plt)", "plt)"
            ) to LaboratoryIndicatorNames.PLATELETS,
            listOf(
                "соэ", "esr", "скорость оседания", "соэ (по вестергрену)", "скорость оседания эритроцит",
                "оседания эритроцитов", "по вестергрену", "подсчет скорости оседания",
                "соэ по вестергрену", "соэ методом вестергрена", "вестергрена"
            ) to LaboratoryIndicatorNames.SOE,
            listOf(
                "mcv", "мсv", "мcv", "ср. объем эритр", "ср.объем эритр", "mcv (ср. объем эритр.)",
                "средний объем эритроцит", "средний объём эритроцит", "(mcv) ср. объем эритроцит", "(mcv)", "mcv)",
                "сред. объем эритр", "ср. объем эритр.", "ср объём эритроцит", "mean corpuscular volume"
            ) to LaboratoryIndicatorNames.MCV,
            listOf(
                "(rdw-c", "rdw-c) ширинараспределения", "(rdw-cширинараспределения",
                "rdw(ширраспредэритр)", "rdw (ширраспредэритр)", "ширраспредэритр",
                "rdw-cv", "(rdw-cv)", "(rdw-cv) ширина распределения эритроцит",
                "rdw-cv) ширина", "rdw cv", "rdw %", "rdw%", "(rdw-cv) шир. распред. эритр",
                "rdw (шир. распред. эритр)", "rdw (шир. распред. эритр.)", "rdw(шир. распред. эритр)",
                "rdw (шир.распред. эритр)", "rdw (шир распред эритр)", "(rdw) шир. распред. эритр",
                "rdw шир. распред. эритр", "rdw шир.распред.эритр", "шир. распред. эритр"
            ) to LaboratoryIndicatorNames.RDW,
            listOf(
                "лимфоциты %", "лимфоциты, %", "(lym%)", "(lym%) лимфоциты",
                "лимфоциты (lym%)", "лимфоцит %", "лимфоцит, %",
                "относительное количество лимфоцит", "отн. кол-во лимфоцит",
                "лимфоциты", "лимфоцит"
            ) to LaboratoryIndicatorNames.LYMPHOCYTES_PERCENT,
            listOf(
                "глюкоз", "glucose", "glu", "глюкоза (венозной", "глюкоза (кровь",
                "глюкоза венозн", "глюкоза кровь", "глюкоза плазмы", "глюкоза сыворотки",
                "глюкоза натощак", "глюкоза fasting", "sugar"
            ) to LaboratoryIndicatorNames.GLUCOSE,
            listOf(
                "лпнп", "ldl", "холестерин лпнп", "ldl-холестерин", "ldl cholesterol",
                "холестерол лпнп", "лпнп холестерин", "липопротеины низкой плотности",
                "липопротеиды низкой плотн"
            ) to LaboratoryIndicatorNames.LDL,
            listOf(
                "лпвп", "hdl", "холестерин лпвп", "hdl-холестерин", "hdl cholesterol",
                "холестерол лпвп", "лпвп холестерин", "липопротеины высокой плотности",
                "липопротеиды высокой плотн"
            ) to LaboratoryIndicatorNames.HDL,
            listOf(
                "холестерин", "cholesterol", "холестерин общий", "общий холестерин",
                "холестерол", "холестерол общий", "total cholesterol", "chol"
            ) to LaboratoryIndicatorNames.CHOLESTEROL,
            listOf(
                "креатинин", "creatinine", "креатинин сыворотки", "креатинин в крови",
                "crea", "креат."
            ) to LaboratoryIndicatorNames.CREATININE,
            listOf(
                "hba1c", "гликированн", "гликированный гемоглобин (hba1c)", "hba1c (гликированный hb)",
                "гликированный гемоглобин", "гликозилированный гемоглобин",
                "гликир. гемоглобин", "гемоглобин a1c"
            ) to LaboratoryIndicatorNames.HBA1C,
            listOf(
                "ттг", "тиреотропн", "tsh", "тиреотропный гормон (ттг)", "ттг (тиротропин)",
                "тиротропин", "тиреотропный гормон", "thyroid stimulating",
                "ттг тиреотропин"
            ) to LaboratoryIndicatorNames.TSH,
            listOf(
                "лдг", "ldh", "лактатдегидрогеназ", "лактатдегидрогеназа (лдг)",
                "лактатдегидрогеназа", "лактат-дегидрогеназа", "lactate dehydrogenase",
                "лдг общая", "ldh общая"
            ) to LaboratoryIndicatorNames.LDH,
            listOf(
                "общий белок", "общего белка", "total protein", "общий белок сыворотки",
                "белок общий", "total protein serum", "протеин общий"
            ) to LaboratoryIndicatorNames.TOTAL_PROTEIN,
            listOf(
                "с-реактивн", "crp", "срб", "с-реактивный белок", "c-reactive protein",
                "crp (с-реактивн", "срб (с-реактивн", "c reactive"
            ) to LaboratoryIndicatorNames.CRP,
            listOf(
                "щелочная фосфатаза", "alkaline phosphatase", "alp", "щелочная фосфатаза (шф)",
                "шф", "alp щелочная", "фосфатаза щелочная", "alkaline phosphatase"
            ) to LaboratoryIndicatorNames.ALKALINE_PHOSPHATASE,
            listOf(
                "альбумин", "albumin", "alb", "альбумин сыворотки", "альбумин в крови",
                "альбумин (alb)", "alb)"
            ) to LaboratoryIndicatorNames.ALBUMIN
        )
    }
}

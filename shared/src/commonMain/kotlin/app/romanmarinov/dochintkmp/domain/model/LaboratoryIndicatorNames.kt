package app.romanmarinov.dochintkmp.domain.model

/**
 * Централизованные канонические названия индикаторов лабораторных анализов.
 * Используются в RuleParser (INDICATOR_MAP, OAK_ACTIVE, BAK_ACTIVE).
 */
object LaboratoryIndicatorNames {
    // ОАК
    const val HEMOGLOBIN = "Гемоглобин"
    const val PLATELETS = "Тромбоциты"
    const val LEUKOCYTES = "Лейкоциты"
    const val LYMPHOCYTES_PERCENT = "Лимфоциты"
    const val SOE = "СОЭ"
    const val MCV = "MCV"
    const val RDW = "RDW"

    // БАК
    const val GLUCOSE = "Глюкоза"
    const val CHOLESTEROL = "Холестерин"
    const val HDL = "ЛПВП"
    const val LDL = "ЛПНП"
    const val HBA1C = "HbA1c"
    const val TSH = "ТТГ"
    const val ALKALINE_PHOSPHATASE = "Щелочная фосфатаза"
    const val LDH = "ЛДГ"
    const val TOTAL_PROTEIN = "Общий белок"
    const val ALBUMIN = "Альбумин"
    const val CREATININE = "Креатинин"
    const val CRP = "С-реактивный белок"

    val OAK_ACTIVE = setOf(
        HEMOGLOBIN, PLATELETS, LEUKOCYTES, LYMPHOCYTES_PERCENT, SOE, MCV, RDW
    )

    val BAK_ACTIVE = setOf(
        GLUCOSE, CHOLESTEROL, HDL, LDL, CREATININE, HBA1C, TSH, LDH,
        TOTAL_PROTEIN, CRP, ALKALINE_PHOSPHATASE, ALBUMIN
    )
}

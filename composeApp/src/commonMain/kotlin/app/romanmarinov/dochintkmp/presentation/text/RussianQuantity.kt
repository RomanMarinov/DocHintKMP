package app.romanmarinov.dochintkmp.presentation.text

/**
 * Склонение слова «анализ» для русского языка (1 анализ, 2–4 анализа, 5 анализов, 11 анализов, 21 анализ, …).
 */
fun russianAnalysisCountLabel(count: Int): String {
    val n100 = count % 100
    val n10 = count % 10
    val word = when {
        n100 in 11..19 -> "анализов"
        n10 == 1 -> "анализ"
        n10 in 2..4 -> "анализа"
        else -> "анализов"
    }
    return "$count $word"
}

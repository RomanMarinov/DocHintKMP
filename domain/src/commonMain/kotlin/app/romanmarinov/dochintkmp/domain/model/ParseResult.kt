package app.romanmarinov.dochintkmp.domain.model

data class ParseResult(
    val housingDocument: HousingPaymentDocument,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val model: String? = null
)

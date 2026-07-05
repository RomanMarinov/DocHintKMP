package app.romanmarinov.dochintkmp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterRequest(
    val model: String = OpenRouterClient.MODEL_TEXT,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.0,
    @SerialName("max_tokens") val maxTokens: Int = 4000
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class Choice(
    val message: Message? = null
)

@Serializable
data class Message(
    val content: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class HousingPaymentDocumentDto(
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("institution") val institution: String? = null,
    @SerialName("document_date") val documentDate: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("payment_document_id") val paymentDocumentId: String? = null,
    @SerialName("personal_account_number") val personalAccountNumber: String? = null,
    @SerialName("unified_personal_account") val unifiedPersonalAccount: String? = null,
    @SerialName("housing_utilities_id") val housingUtilitiesId: String? = null,
    @SerialName("property_address") val propertyAddress: String? = null,
    @SerialName("payer_name") val payerName: String? = null,
    @SerialName("total_area_sqm") val totalAreaSqm: String? = null,
    @SerialName("living_area_sqm") val livingAreaSqm: String? = null,
    @SerialName("residents_count") val residentsCount: String? = null,
    @SerialName("amount_due_for_period") val amountDueForPeriod: String? = null,
    @SerialName("amount_paid") val amountPaid: String? = null,
    @SerialName("last_payment_date") val lastPaymentDate: String? = null,
    @SerialName("debt_from_previous_periods") val debtFromPreviousPeriods: String? = null,
    @SerialName("service_lines") val serviceLines: List<HousingServiceLineDto>? = null
)

@Serializable
data class HousingServiceLineDto(
    val name: String? = null,
    val group: String? = null,
    val unit: String? = null,
    val volume: String? = null,
    @SerialName("volume_basis") val volumeBasis: String? = null,
    val tariff: String? = null,
    @SerialName("amount_to_pay") val amountToPay: String? = null
)

@Serializable
data class KeyInfoResponse(val data: KeyInfoData? = null)

@Serializable
data class KeyInfoData(
    val label: String? = null,
    val usage: Double = 0.0,
    val limit: Double? = null,
    @SerialName("limit_remaining") val limitRemaining: Double? = null,
    @SerialName("is_free_tier") val isFreeTier: Boolean = false,
    @SerialName("rate_limit") val rateLimit: RateLimit? = null
)

@Serializable
data class RateLimit(
    val requests: Int = 0,
    val interval: String? = null
)


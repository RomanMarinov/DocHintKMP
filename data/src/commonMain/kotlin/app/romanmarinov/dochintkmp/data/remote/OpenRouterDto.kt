package app.romanmarinov.dochintkmp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterRequest(
    val model: String = OpenRouterClient.MODEL_TEXT,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.0,
    @SerialName("max_tokens") val maxTokens: Int = 2500
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
data class MedicalDataDto(
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("institution") val institution: String? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("analysis_date") val analysisDate: String? = null,
    @SerialName("indicators") val indicators: List<IndicatorDto>? = null
)

@Serializable
data class IndicatorDto(
    val name: String? = null,
    val value: String? = null,
    @SerialName("reference_range") val referenceRange: String? = null
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


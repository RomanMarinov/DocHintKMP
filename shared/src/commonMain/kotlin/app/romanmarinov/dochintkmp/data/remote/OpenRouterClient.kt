package app.romanmarinov.dochintkmp.data.remote

import app.romanmarinov.dochintkmp.data.mapper.toDomain
import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.data.remote.KeyInfoData
import app.romanmarinov.dochintkmp.data.remote.KeyInfoResponse
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.util.logDebug
import app.romanmarinov.dochintkmp.util.logError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.plugins.ClientRequestException
import kotlinx.serialization.json.Json

class OpenRouterClient(
    private val httpClient: HttpClient
) {
    suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult {
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        val request = OpenRouterRequest(
            messages = listOf(OpenRouterMessage(role = "user", content = "$SYSTEM_PROMPT\n\n---\n\n$cleanText"))
        )

        val response: OpenRouterResponse = try {
            httpClient.post("${BASE_URL}api/v1/chat/completions") {
                header("Authorization", authHeader)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            throw IllegalStateException(
                when (code) {
                    401 -> "Неверный API ключ (401). Проверьте ключ в Настройках."
                    402 -> "Недостаточно средств (402). Пополните баланс OpenRouter."
                    429 -> "Слишком много запросов (429). Подождите минуту."
                    else -> "Ошибка API: $code."
                }
            )
        }

        response.error?.let { err ->
            throw IllegalStateException("OpenRouter: ${err.message ?: "Неизвестная ошибка API"}")
        }

        val json = response.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Модель вернула пустой ответ. Попробуйте ещё раз.")

        val domainData = parseResponse(json)
        validateDocumentType(domainData)

        val usage = response.usage
        return ParseResult(
            data = domainData,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
            model = MODEL_TEXT
        )
    }

    suspend fun getKeyInfo(apiKey: String): KeyInfoData? {
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        return try {
            val response: KeyInfoResponse = httpClient.get("${BASE_URL}api/v1/key") {
                header("Authorization", authHeader)
            }.body()
            response.data
        } catch (e: Exception) {
            logError("OpenRouterClient", "getKeyInfo failed", e)
            null
        }
    }

    private fun parseResponse(raw: String): MedicalData {
        val extracted = extractJson(raw)
        return try {
            val dto = json.decodeFromString<MedicalDataDto>(extracted)
            normalizeDocumentType(dto.toDomain())
        } catch (e: Exception) {
            if (extracted.contains("},{")) {
                val repaired = repairTruncatedJson(extracted)
                if (repaired != null) {
                    try {
                        val dto = json.decodeFromString<MedicalDataDto>(repaired)
                        return normalizeDocumentType(dto.toDomain())
                    } catch (_: Exception) { }
                }
            }
            logError("OpenRouterClient", "parseResponse failed: ${e.message}", e)
            logDebug("OpenRouterClient", "extracted (first 500): ${extracted.take(500)}")
            throw IllegalStateException(
                "Не удалось распознать данные в документе. Проверьте, что документ — анализ крови."
            )
        }
    }

    private fun extractJson(raw: String): String {
        var s = raw.trim()
            .replace(Regex("""^```(?:json)?\s*"""), "")
            .replace(Regex("""\s*```$"""), "")
            .trim()
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw IllegalArgumentException("Ответ модели не содержит JSON")
        }
        s = s.substring(start, end + 1)
        return repairJson(s)
    }

    private fun repairJson(str: String): String =
        str.replace(Regex(""",\s*([}\]])"""), "$1")
            .replace(Regex("""[\u0000-\u001F]"""), " ")

    private fun repairTruncatedJson(json: String): String? {
        val lastComplete = json.lastIndexOf("},{")
        if (lastComplete < 0) return null
        return json.substring(0, lastComplete + 1) + "]}"
    }

    private fun validateDocumentType(data: MedicalData) {
        var type = data.documentType?.trim()?.lowercase()
        if (type.isNullOrBlank() && !data.indicators.isNullOrEmpty()) {
            type = inferDocumentType(data.indicators)
        }
        if (type.isNullOrBlank()) {
            throw IllegalStateException("Не удалось распознать данные в документе")
        }
        val isOak = type.contains("оак") || type.contains("oak") || type.contains("общий анализ") || type.contains("клинический")
        val isBak = type.contains("бак") || type.contains("bak") || type.contains("биохим")
        if (!isOak && !isBak) {
            throw IllegalStateException("Данный тип анализа не поддерживается")
        }
    }

    private fun inferDocumentType(indicators: List<AnalysisIndicator>): String? {
        val oakNames = setOf("гемоглобин", "эритроциты", "лейкоциты", "тромбоциты", "соэ", "нейтрофилы", "mcv", "rdw")
        val bakNames = setOf("глюкоза", "холестерин", "алт", "аст", "креатинин", "билирубин", "мочевина", "калий", "натрий")
        val names = indicators.map { it.name.lowercase() }.toSet()
        val oakCount = names.count { n -> oakNames.any { n.contains(it) } }
        val bakCount = names.count { n -> bakNames.any { n.contains(it) } }
        return when {
            oakCount >= bakCount && oakCount > 0 -> "ОАК"
            bakCount > 0 -> "Биохимический анализ крови"
            else -> null
        }
    }

    private fun normalizeDocumentType(data: MedicalData): MedicalData {
        val t = data.documentType?.trim()?.lowercase()
        val normalized = when {
            t == null || t.isBlank() -> data.documentType
            t == "oak" || t == "оак" || t.contains("общий анализ") || t.contains("клинический") -> "ОАК"
            t == "bak" || t == "бак" || t.contains("биохим") -> "Биохимический анализ крови"
            else -> data.documentType
        }
        return data.copy(documentType = normalized)
    }

    companion object {
        const val BASE_URL = "https://openrouter.ai/"
        const val MODEL_TEXT = "google/gemini-2.0-flash-001"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private const val SYSTEM_PROMPT =
            "Ты — парсер медицинских анализов крови. КРИТИЧНО: для ОАК в таблице есть строки «Эозинофилы, %», «Эозинофилы, абс.», «Базофилы, %», «Базофилы, абс.» — это 4 РАЗНЫХ показателя, извлеки все 4. Верни ТОЛЬКО JSON.\n" +
            "Поддерживаемые типы: \"ОАК\" (общий анализ крови) и \"Биохимический анализ крови\".\n" +
            "Определи тип по ключевым словам:\n" +
            "- ОАК: \"общий анализ крови\", \"ОАК\", \"клинический анализ крови\"\n" +
            "- Биохимия: \"биохимия\", \"биохимический\", \"БАК\"\n\n" +
            "Формат ответа:\n" +
            "{\"document_type\":\"ОАК\"|\"Биохимический анализ крови\"|null," +
            "\"institution\":string|null,\"doctor_name\":string|null," +
            "\"analysis_date\":string|null," +
            "\"indicators\":[{\"name\":string,\"value\":string,\"reference_range\":string|null}]}\n\n" +
            "Показатели для ОАК и Биохимии — извлекай каждый показатель из документа. " +
            "ПЕРЕД ОТВЕТОМ проверь: в indicators есть и \"Эозинофилы, %\" и \"Эозинофилы, абс.\", и \"Базофилы, %\" и \"Базофилы, абс.\"? Если в таблице есть эти 4 строки — все 4 должны быть в JSON.\n" +
            "- Без markdown, без объяснений, ТОЛЬКО JSON"
    }
}

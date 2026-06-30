package app.romanmarinov.dochintkmp.data.remote

import app.romanmarinov.dochintkmp.data.mapper.toDomain
import app.romanmarinov.dochintkmp.domain.model.AnalysisIndicator
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
            // simplified logging to avoid cross-module dependency
            println("OpenRouterClient getKeyInfo failed: ${e.message}")
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
            println("OpenRouterClient parseResponse failed: ${e.message}")
            println("OpenRouterClient extracted (first 500): ${extracted.take(500)}")
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

        val indicators = data.indicators
        if (type.isNullOrBlank() && !indicators.isNullOrEmpty()) {
            type = inferDocumentType(indicators)
        }

        if (type.isNullOrBlank()) {
            throw IllegalStateException("Не удалось распознать данные в документе")
        }
        
        val isOak = type.contains("оак") || type.contains("oak") || type.contains("общий анализ") || type.contains("клинический")
        val isBak = type.contains("бак") || type.contains("bak") || type.contains("биохим")
        val isHousing = type.contains("квитанция") || type.contains("жку") || type.contains("жкх") || 
                       type.contains("коммунальн") || type.contains("платёж") || type.contains("платеж")
        
        if (!isOak && !isBak && !isHousing) {
            throw IllegalStateException("Данный тип документа не поддерживается. Используйте анализы крови или квитанции ЖКХ")
        }
    }

    private fun inferDocumentType(indicators: List<AnalysisIndicator>): String? {
        val oakNames = setOf("гемоглобин", "эритроциты", "лейкоциты", "тромбоциты", "соэ", "нейтрофилы", "mcv", "rdw", "эозинофилы", "базофилы")
        val bakNames = setOf("глюкоза", "холестерин", "алт", "аст", "креатинин", "билирубин", "мочевина", "калий", "натрий")
        val housingNames = setOf("содержание", "отопление", "водоснабжение", "электричество", "газоснабжение", "вывоз", "водоотведение", "услуга", "тариф")
        
        val names = indicators.map { it.name.lowercase() }.toSet()
        val oakCount = names.count { n -> oakNames.any { n.contains(it) } }
        val bakCount = names.count { n -> bakNames.any { n.contains(it) } }
        val housingCount = names.count { n -> housingNames.any { n.contains(it) } }
        
        return when {
            housingCount >= 2 -> "Квитанция ЖКХ"
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
            t.contains("квитанция") || t.contains("жку") || t.contains("жкх") || 
            t.contains("коммунальн") || t.contains("платёж") || t.contains("платеж") -> 
                if (t.contains("капремонт") || t.contains("капитальный")) "Квитанция ЖКХ (капремонт)" 
                else "Квитанция ЖКХ (основная)"
            else -> data.documentType
        }
        return data.copy(documentType = normalized)
    }

    companion object {
        const val BASE_URL = "https://openrouter.ai/"
        const val MODEL_TEXT = "gpt-4o-mini"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private const val SYSTEM_PROMPT =
            "Ты — универсальный парсер документов. Извлекаешь структурированные данные и НЕ СМЕШИВАЕШЬ поля между типами документов.\n\n" +
            "ПОДДЕРЖИВАЕМЫЕ ТИПЫ ДОКУМЕНТОВ:\n" +
            "1. ОАК (общий анализ крови)\n" +
            "2. БАК (биохимический анализ крови)\n" +
            "3. ЖКХ (квитанции за коммунальные услуги)\n\n" +
            "ВАЖНО для ОАК: Эозинофилы и Базофилы имеют ДВА показателя каждые: \"..., %\" и \"..., абс.\" — это разные значения, извлеки ОБА!\n\n" +
            "ОПРЕДЕЛИ ТИП ДОКУМЕНТА:\n" +
            "- ОАК: содержит гемоглобин, эритроциты, лейкоциты, тромбоциты, СОЭ\n" +
            "- БАК: содержит глюкозу, холестерин, билирубин, АЛТ, АСТ, креатинин\n" +
            "- ЖКХ: содержит услуги (отопление, водоснабжение, электричество), УК, плательщика, квитанцию\n\n" +
            "ФОРМАТ JSON-ответа (ОБЯЗАТЕЛЬНО ПРАВИЛЬНЫЕ ПОЛЯ ДЛЯ КАЖДОГО ТИПА):\n\n" +
            "==== ПРИМЕР ДЛЯ ОАК ====\n" +
            "{\n" +
            "  \"document_type\": \"ОАК\",\n" +
            "  \"institution\": \"ИНВИТРО лаборатория\",\n" +
            "  \"doctor_name\": \"Иванов И.И.\",\n" +
            "  \"analysis_date\": \"15.10.2024\",\n" +
            "  \"indicators\": [\n" +
            "    {\"name\": \"Гемоглобин\", \"value\": \"120 г/л\", \"reference_range\": \"120-160\"},\n" +
            "    {\"name\": \"Эритроциты\", \"value\": \"4.2 млн/мкл\", \"reference_range\": \"4.0-5.0\"},\n" +
            "    {\"name\": \"Эозинофилы, %\", \"value\": \"2%\", \"reference_range\": \"0-5%\"},\n" +
            "    {\"name\": \"Эозинофилы, абс.\", \"value\": \"150 /мкл\", \"reference_range\": \"0-400\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "==== ПРИМЕР ДЛЯ ЖКХ ====\n" +
            "{\n" +
            "  \"document_type\": \"Квитанция ЖКХ (основная)\",\n" +
            "  \"institution\": \"ООО УК 'Дом'\",\n" +
            "  \"doctor_name\": \"Петров Сергей Викторович\",\n" +
            "  \"analysis_date\": \"2024-10\",\n" +
            "  \"indicators\": [\n" +
            "    {\"name\": \"Содержание и ремонт\", \"value\": \"2500 ₽\", \"reference_range\": \"12 кв.м\"},\n" +
            "    {\"name\": \"Отопление\", \"value\": \"3100 ₽\", \"reference_range\": \"0.5 Гкал\"},\n" +
            "    {\"name\": \"Холодное водоснабжение\", \"value\": \"800 ₽\", \"reference_range\": \"5 куб.м\"},\n" +
            "    {\"name\": \"Электроэнергия\", \"value\": \"1200 ₽\", \"reference_range\": \"120 кВт·ч\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "ПРАВИЛА ЗАПОЛНЕНИЯ:\n" +
            "- document_type: точное название типа документа (ОАК, БАК, Квитанция ЖКХ (основная) или Квитанция ЖКХ (капремонт))\n" +
            "- institution: для анализов=лаборатория, для ЖКХ=управляющая компания\n" +
            "- doctor_name: для анализов=врач, для ЖКХ=ПЛАТЕЛЬЩИК (ФИО хозяина квартиры)\n" +
            "- analysis_date: для анализов=дата анализа (ДД.ММ.ГГГГ), для ЖКХ=период платежа (ГГГГ-ММ)\n" +
            "- indicators: показатели с name, value, reference_range\n\n" +
            "ОБЯЗАТЕЛЬНО:\n" +
            "1. Верни ТОЛЬКО JSON-объект, БЕЗ markdown кода (без ```), БЕЗ объяснений\n" +
            "2. Не добавляй лишние поля в JSON\n" +
            "3. indicators ОБЯЗАТЕЛЬНО должен быть непустой массив\n" +
            "4. НЕ СМЕШИВАЙ поля между типами (не пиши врача для ЖКХ, не пиши услуги для анализов)"
    }
}


package app.romanmarinov.dochintkmp.data.remote

import app.romanmarinov.dochintkmp.data.mapper.toDomain
import app.romanmarinov.dochintkmp.domain.model.HousingBillCategory
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
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

        val normalized = parseResponse(json)

        val usage = response.usage
        return ParseResult(
            housingDocument = normalized,
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
            println("OpenRouterClient getKeyInfo failed: ${e.message}")
            null
        }
    }

    private fun parseResponse(raw: String): HousingPaymentDocument {
        val extracted = extractJson(raw)
        return try {
            val dto = json.decodeFromString<HousingPaymentDocumentDto>(extracted)
            val doc = dto.toDomain()
            validateHousingDocument(doc)
            normalizeHousingDocument(doc)
        } catch (e: Exception) {
            val repaired = repairTruncatedJson(extracted)
            if (repaired != null) {
                try {
                    val dto = json.decodeFromString<HousingPaymentDocumentDto>(repaired)
                    val doc = dto.toDomain()
                    validateHousingDocument(doc)
                    return normalizeHousingDocument(doc)
                } catch (_: Exception) { }
            }
            println("OpenRouterClient parseResponse failed: ${e.message}")
            println("OpenRouterClient extracted (first 500): ${extracted.take(500)}")
            throw IllegalStateException(
                "Не удалось распознать данные в квитанции. Проверьте, что документ — квитанция ЖКХ."
            )
        }
    }

    private fun validateHousingDocument(doc: HousingPaymentDocument) {
        val type = doc.documentType.lowercase()
        if (type.contains("оак") || type.contains("бак") || type.contains("анализ")) {
            throw IllegalStateException("Документ похож на медицинский анализ. Приложение поддерживает только квитанции ЖКХ.")
        }
        if (doc.documentType.isBlank()) {
            throw IllegalStateException("Не удалось распознать тип документа. Проверьте, что документ — квитанция ЖКХ.")
        }
        if (doc.serviceLines.isNullOrEmpty()) {
            throw IllegalStateException("Не найдены услуги в квитанции. Проверьте, что документ — квитанция ЖКХ.")
        }
    }

    private fun normalizeHousingDocument(doc: HousingPaymentDocument): HousingPaymentDocument {
        val rawType = doc.documentType.lowercase()
        val isCapital = rawType.contains("капремонт") || rawType.contains("капитальный")
        val category = if (isCapital) HousingBillCategory.CAPITAL_REPAIR else doc.category
        return doc.copy(
            documentType = "Квитанция ЖКУ",
            category = category
        )
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

    companion object {
        const val BASE_URL = "https://openrouter.ai/"
        const val MODEL_TEXT = "gpt-4o-mini"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private const val SYSTEM_PROMPT =
            "Ты — парсер квитанций за коммунальные услуги (ЖКХ). Извлекаешь структурированные данные в JSON.\n\n" +
            "ПОДДЕРЖИВАЕМЫЙ ТИП ДОКУМЕНТА:\n" +
            "- ЖКХ (квитанции за коммунальные услуги: основная квитанция и квитанция капремонта)\n\n" +
            "ОПРЕДЕЛИ ТИП ДОКУМЕНТА:\n" +
            "- ЖКХ: содержит услуги (отопление, водоснабжение, электричество, газоснабжение, водоотведение), управляющую компанию, плательщика, лицевой счёт, адрес.\n\n" +
            "ФОРМАТ JSON-ответа (ПОЛНАЯ СХЕМА — ОБЯЗАТЕЛЬНО ИЗВЛЕКАЙ ВСЕ ДОСТУПНЫЕ ПОЛЯ):\n\n" +
            "==== ПРИМЕР ДЛЯ ЖКХ ====\n" +
            "{\n" +
            "  \"document_type\": \"Квитанция ЖКХ\",\n" +
            "  \"institution\": \"ООО УК 'Дом'\",\n" +
            "  \"document_date\": \"2024-10\",\n" +
            "  \"category\": \"MAIN\",\n" +
            "  \"document_number\": \"12345\",\n" +
            "  \"payment_document_id\": \"ЕРЦ-001\",\n" +
            "  \"personal_account_number\": \"40-0001-01\",\n" +
            "  \"unified_personal_account\": \"000000000000\",\n" +
            "  \"housing_utilities_id\": \"1234567\",\n" +
            "  \"property_address\": \"г. Москва, ул. Ленина, д. 1, кв. 1\",\n" +
            "  \"payer_name\": \"Петров Сергей Викторович\",\n" +
            "  \"total_area_sqm\": \"60.5\",\n" +
            "  \"living_area_sqm\": \"42.0\",\n" +
            "  \"residents_count\": \"3\",\n" +
            "  \"amount_due_for_period\": \"7600\",\n" +
            "  \"amount_paid\": \"7600\",\n" +
            "  \"last_payment_date\": \"10.10.2024\",\n" +
            "  \"debt_from_previous_periods\": \"0\",\n" +
            "  \"service_lines\": [\n" +
            "    {\"name\": \"Содержание и ремонт\", \"group\": \"MAINTENANCE\", \"unit\": \"кв.м\", \"volume\": \"60.5\", \"volume_basis\": \"METER\", \"tariff\": \"28.50\", \"amount_to_pay\": \"1724\"},\n" +
            "    {\"name\": \"Отопление\", \"group\": \"UTILITIES\", \"unit\": \"Гкал\", \"volume\": \"0.85\", \"volume_basis\": \"METER\", \"tariff\": \"2500.00\", \"amount_to_pay\": \"2125\"},\n" +
            "    {\"name\": \"Холодное водоснабжение\", \"group\": \"UTILITIES\", \"unit\": \"куб.м\", \"volume\": \"5\", \"volume_basis\": \"METER\", \"tariff\": \"38.48\", \"amount_to_pay\": \"192\"},\n" +
            "    {\"name\": \"Электроэнергия\", \"group\": \"UTILITIES\", \"unit\": \"кВт·ч\", \"volume\": \"120\", \"volume_basis\": \"METER\", \"tariff\": \"5.38\", \"amount_to_pay\": \"646\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "ПРАВИЛА ЗАПОЛНЕНИЯ:\n" +
            "- Используй ТОЛЬКО поля схемы выше с service_lines. НЕ возвращай indicators, doctor_name, analysis_date.\n" +
            "- document_type: точное название типа документа (Квитанция ЖКХ). Для капремонта — \"Квитанция ЖКХ (капремонт)\".\n" +
            "- institution: управляющая компания/исполнитель (УК, ТСЖ, ЕРЦ и т.п.).\n" +
            "- payer_name: ФИО плательщика.\n" +
            "- document_date: период платежа (ГГГГ-ММ или ММ.ГГГГ).\n" +
            "- property_address: адрес помещения.\n" +
            "- category: MAIN или CAPITAL_REPAIR (для квитанций капремонта).\n" +
            "- group услуги: MAINTENANCE (содержание/ремонт), COMMON_PROPERTY_ODN (ОДН), UTILITIES (коммунальные: вода, отопление, газ, электричество, водоотведение), ADDITIONAL, CAPITAL_REPAIR.\n" +
            "- volume_basis: METER (по счётчику), NORM (по нормативу), OTHER.\n" +
            "- Извлекай ВСЕ суммы, тарифы, объёмы и единицы измерения как есть (числом, без символа ₽).\n" +
            "- Если какого-то поля нет в документе — опусти его или верни null. НЕ ВЫДУМЫВАЙ значения.\n\n" +
            "ОБЯЗАТЕЛЬНО:\n" +
            "1. Верни ТОЛЬКО JSON-объект, БЕЗ markdown кода (без ```), БЕЗ объяснений\n" +
            "2. Не добавляй лишние поля в JSON\n" +
            "3. service_lines ОБЯЗАТЕЛЬНО непустой массив\n" +
            "4. Используй точные имена полей в snake_case"
    }
}

import Foundation

/// Swift implementation of OpenRouter API for AI Scanner (mirrors KMP OpenRouterClient).
final class OpenRouterService {
    private let baseURL = "https://openrouter.ai/"
    private let model = "gpt-4o-mini"

    private let systemPrompt = """
    Ты — парсер квитанций за коммунальные услуги (ЖКХ). Извлекай структурированные данные в JSON.
    Поддерживаемые типы: "Квитанция ЖКХ" и "Квитанция ЖКХ (капремонт)".
    Определи тип документа по ключевым словам:
    - ЖКХ: содержит услуги (отопление, водоснабжение, электричество, газоснабжение, водоотведение), управляющую компанию, плательщика, лицевой счёт, адрес.

    Формат ответа:
    {"document_type":"Квитанция ЖКХ"|"Квитанция ЖКХ (капремонт)","institution":string|null,"document_date":string|null,"category":"MAIN"|"CAPITAL_REPAIR"|null,"document_number":string|null,"payment_document_id":string|null,"personal_account_number":string|null,"unified_personal_account":string|null,"housing_utilities_id":string|null,"property_address":string|null,"payer_name":string|null,"total_area_sqm":string|null,"living_area_sqm":string|null,"residents_count":string|null,"amount_due_for_period":string|null,"amount_paid":string|null,"last_payment_date":string|null,"debt_from_previous_periods":string|null,"service_lines":[{"name":string,"group":"MAINTENANCE"|"COMMON_PROPERTY_ODN"|"UTILITIES"|"ADDITIONAL"|"CAPITAL_REPAIR","unit":string|null,"volume":string|null,"volume_basis":"METER"|"NORM"|"OTHER"|null,"tariff":string|null,"amount_to_pay":string}]}"

    ПРАВИЛА ЗАПОЛНЕНИЯ:
    - Используй ТОЛЬКО поля схемы выше. НЕ возвращай indicators, doctor_name, analysis_date.
    - document_type: "Квитанция ЖКХ" или "Квитанция ЖКХ (капремонт)".
    - institution: управляющая компания/исполнитель (УК, ТСЖ, ЕРЦ и т.п.). Сохраняй полное название, не сокращай до "ООО УК".
    - document_date: период платежа (ГГГГ-ММ или ММ.ГГГГ).
    - category: MAIN или CAPITAL_REPAIR.
    - service_lines обязательно непустой массив.
    - Если поля нет — верни null или опусти его.
    - В ответе только JSON, без markdown, без объяснений, без лишних полей.
    """

    struct ParseResponse: Decodable {
        let choices: [Choice]?
        let usage: Usage?
        let error: ApiError?
    }
    struct Choice: Decodable { let message: Message? }
    struct Message: Decodable { let content: String? }
    struct Usage: Decodable {
        let promptTokens: Int?
        let completionTokens: Int?
        let totalTokens: Int?
        enum CodingKeys: String, CodingKey {
            case promptTokens = "prompt_tokens"
            case completionTokens = "completion_tokens"
            case totalTokens = "total_tokens"
        }
    }
    struct ApiError: Decodable { let message: String? }

    struct HousingPaymentDocumentDto: Decodable {
        let documentType: String?
        let institution: String?
        let documentDate: String?
        let category: String?
        let documentNumber: String?
        let paymentDocumentId: String?
        let personalAccountNumber: String?
        let unifiedPersonalAccount: String?
        let housingUtilitiesId: String?
        let propertyAddress: String?
        let payerName: String?
        let totalAreaSqm: String?
        let livingAreaSqm: String?
        let residentsCount: String?
        let amountDueForPeriod: String?
        let amountPaid: String?
        let lastPaymentDate: String?
        let debtFromPreviousPeriods: String?
        let serviceLines: [HousingServiceLineDto]?
        enum CodingKeys: String, CodingKey {
            case documentType = "document_type"
            case institution
            case documentDate = "document_date"
            case category
            case documentNumber = "document_number"
            case paymentDocumentId = "payment_document_id"
            case personalAccountNumber = "personal_account_number"
            case unifiedPersonalAccount = "unified_personal_account"
            case housingUtilitiesId = "housing_utilities_id"
            case propertyAddress = "property_address"
            case payerName = "payer_name"
            case totalAreaSqm = "total_area_sqm"
            case livingAreaSqm = "living_area_sqm"
            case residentsCount = "residents_count"
            case amountDueForPeriod = "amount_due_for_period"
            case amountPaid = "amount_paid"
            case lastPaymentDate = "last_payment_date"
            case debtFromPreviousPeriods = "debt_from_previous_periods"
            case serviceLines = "service_lines"
        }
    }
    struct HousingServiceLineDto: Decodable {
        let name: String?
        let group: String?
        let unit: String?
        let volume: String?
        let volumeBasis: String?
        let tariff: String?
        let amountToPay: String?
        enum CodingKeys: String, CodingKey {
            case name, group, unit, volume, tariff
            case volumeBasis = "volume_basis"
            case amountToPay = "amount_to_pay"
        }
    }

    func parseWithLlm(apiKey: String, cleanText: String) async throws -> HousingPaymentDocument {
        let authHeader = apiKey.hasPrefix("Bearer ") ? apiKey : "Bearer \(apiKey)"
        let url = URL(string: "\(baseURL)api/v1/chat/completions")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(authHeader, forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "model": model,
            "messages": [
                ["role": "user", "content": "\(systemPrompt)\n\n---\n\n\(cleanText)"]
            ],
            "temperature": 0.0,
            "max_tokens": 2500
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AIError.unknown }

        if http.statusCode == 401 { throw AIError.invalidKey }
        if http.statusCode == 402 { throw AIError.insufficientFunds }
        if http.statusCode == 429 { throw AIError.rateLimit }

        let parsed = try JSONDecoder().decode(ParseResponse.self, from: data)
        if let err = parsed.error?.message { throw AIError.apiError(err) }

        guard let content = parsed.choices?.first?.message?.content else {
            throw AIError.emptyResponse
        }

        let json = extractJson(from: content)
        let dto = try JSONDecoder().decode(HousingPaymentDocumentDto.self, from: json.data(using: .utf8)!)
        let serviceLines = (dto.serviceLines ?? []).compactMap { toHousingServiceLine($0) }
        if serviceLines.isEmpty {
            throw AIError.noHousingBillData
        }

        let category = normalizeCategory(dto.category)
        let documentType = normalizeDocumentType(dto.documentType, category: category)

        return HousingPaymentDocument(
            documentType: documentType,
            institution: dto.institution,
            documentDate: dto.documentDate,
            source: nil,
            category: category,
            documentNumber: dto.documentNumber,
            paymentDocumentId: dto.paymentDocumentId,
            personalAccountNumber: dto.personalAccountNumber,
            unifiedPersonalAccount: dto.unifiedPersonalAccount,
            housingUtilitiesId: dto.housingUtilitiesId,
            propertyAddress: dto.propertyAddress,
            payerName: dto.payerName,
            totalAreaSqm: dto.totalAreaSqm,
            livingAreaSqm: dto.livingAreaSqm,
            residentsCount: dto.residentsCount,
            amountDueForPeriod: dto.amountDueForPeriod,
            amountPaid: dto.amountPaid,
            lastPaymentDate: dto.lastPaymentDate,
            debtFromPreviousPeriods: dto.debtFromPreviousPeriods,
            serviceLines: serviceLines
        )
    }

    private func extractJson(from raw: String) -> String {
        let s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let start = s.firstIndex(of: "{"),
              let end = s.lastIndex(of: "}") else {
            return raw
        }
        return String(s[start...end])
    }

    private func normalizeDocumentType(_ t: String?, category: HousingBillCategory) -> String {
        let raw = (t ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if raw.isEmpty {
            return category == .capitalRepair ? "Квитанция ЖКХ (капремонт)" : "Квитанция ЖКХ"
        }
        return raw
    }

    private func normalizeCategory(_ raw: String?) -> HousingBillCategory {
        let value = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if value.contains("капремонт") || value.contains("капремон") || value.contains("капит") {
            return .capitalRepair
        }
        return .main
    }

    private func toHousingServiceLine(_ dto: HousingServiceLineDto) -> HousingServiceLine? {
        guard let name = dto.name, !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let amountToPay = dto.amountToPay, !amountToPay.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let group = normalizeServiceGroup(dto.group) else {
            return nil
        }

        return HousingServiceLine(
            name: name,
            group: group,
            unit: dto.unit,
            volume: dto.volume,
            volumeBasis: normalizeVolumeBasis(dto.volumeBasis),
            tariff: dto.tariff,
            amountToPay: amountToPay
        )
    }

    private func normalizeServiceGroup(_ raw: String?) -> HousingServiceGroup? {
        let lower = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if lower.contains("содерж") || lower.contains("ремонт") || lower.contains("капрем") {
            return .maintenance
        }
        if lower.contains("одн") || lower.contains("обща") || lower.contains("внеп") {
            return .commonPropertyOdn
        }
        if lower.contains("электр") || lower.contains("газ") || lower.contains("вод") || lower.contains("тепл") || lower.contains("отопл") {
            return .utilities
        }
        if lower.contains("доп") || lower.contains("пени") || lower.contains("штраф") {
            return .additional
        }
        return .utilities
    }

    private func normalizeVolumeBasis(_ raw: String?) -> HousingVolumeBasis? {
        let lower = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if lower.contains("норм") || lower.contains("норма") {
            return .norm
        }
        if lower.contains("м") || lower.contains("куб") || lower.contains("гкал") {
            return .meter
        }
        if lower.isEmpty { return nil }
        return .other
    }

    enum AIError: LocalizedError {
        case invalidKey
        case insufficientFunds
        case rateLimit
        case emptyResponse
        case noHousingBillData
        case apiError(String)
        case unknown

        var errorDescription: String? {
            switch self {
            case .invalidKey: return "Неверный API ключ"
            case .insufficientFunds: return "Недостаточно средств"
            case .rateLimit: return "Слишком много запросов"
            case .emptyResponse: return "Пустой ответ"
            case .noHousingBillData: return "Не удалось распознать данные ЖКХ"
            case .apiError(let m): return m
            case .unknown: return "Неизвестная ошибка"
            }
        }
    }
}

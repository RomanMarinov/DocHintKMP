import Foundation

/// Swift implementation of OpenRouter API for AI Scanner (mirrors KMP OpenRouterClient).
final class OpenRouterService {
    private let baseURL = "https://openrouter.ai/"
    private let model = "google/gemini-2.0-flash-001"

    private let systemPrompt = """
    Ты — парсер медицинских анализов крови. КРИТИЧНО: для ОАК в таблице есть строки «Эозинофилы, %», «Эозинофилы, абс.», «Базофилы, %», «Базофилы, абс.» — это 4 РАЗНЫХ показателя, извлеки все 4. Верни ТОЛЬКО JSON.
    Поддерживаемые типы: "ОАК" (общий анализ крови) и "Биохимический анализ крови".
    Определи тип по ключевым словам:
    - ОАК: "общий анализ крови", "ОАК", "клинический анализ крови"
    - Биохимия: "биохимия", "биохимический", "БАК"

    Формат ответа:
    {"document_type":"ОАК"|"Биохимический анализ крови"|null,"institution":string|null,"doctor_name":string|null,"analysis_date":string|null,"indicators":[{"name":string,"value":string,"reference_range":string|null}]}

    Показатели для ОАК и Биохимии — извлекай каждый показатель из документа. Без markdown, без объяснений, ТОЛЬКО JSON
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

    struct MedicalDataDto: Decodable {
        let documentType: String?
        let institution: String?
        let doctorName: String?
        let analysisDate: String?
        let indicators: [IndicatorDto]?
        enum CodingKeys: String, CodingKey {
            case documentType = "document_type"
            case institution, doctorName = "doctor_name"
            case analysisDate = "analysis_date"
            case indicators
        }
    }
    struct IndicatorDto: Decodable {
        let name: String?
        let value: String?
        let referenceRange: String?
        enum CodingKeys: String, CodingKey {
            case name, value
            case referenceRange = "reference_range"
        }
    }

    func parseWithLlm(apiKey: String, cleanText: String) async throws -> AIMedicalData {
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
        let dto = try JSONDecoder().decode(MedicalDataDto.self, from: json.data(using: .utf8)!)

        let docType = normalizeDocumentType(dto.documentType)
        let indicators = (dto.indicators ?? [])
            .filter { !($0.name ?? "").isEmpty && !($0.value ?? "").isEmpty }
            .map { AIMedicalIndicator(
                name: $0.name ?? "",
                value: $0.value ?? "",
                referenceRange: $0.referenceRange
            ) }

        if docType?.isEmpty != false && indicators.isEmpty {
            throw AIError.noMedicalData
        }

        return AIMedicalData(
            documentType: docType ?? "Анализ",
            institution: dto.institution,
            doctorName: dto.doctorName,
            analysisDate: dto.analysisDate,
            indicators: indicators
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

    private func normalizeDocumentType(_ t: String?) -> String? {
        let lower = (t ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if lower.isEmpty { return t }
        if lower == "oak" || lower == "оак" || lower.contains("общий анализ") || lower.contains("клинический") {
            return "ОАК"
        }
        if lower == "bak" || lower == "бак" || lower.contains("биохим") {
            return "Биохимический анализ крови"
        }
        return t
    }

    enum AIError: LocalizedError {
        case invalidKey
        case insufficientFunds
        case rateLimit
        case emptyResponse
        case noMedicalData
        case apiError(String)
        case unknown

        var errorDescription: String? {
            switch self {
            case .invalidKey: return "Неверный API ключ"
            case .insufficientFunds: return "Недостаточно средств"
            case .rateLimit: return "Слишком много запросов"
            case .emptyResponse: return "Пустой ответ"
            case .noMedicalData: return "Не удалось распознать данные"
            case .apiError(let m): return m
            case .unknown: return "Неизвестная ошибка"
            }
        }
    }
}

struct AIMedicalData {
    let documentType: String
    let institution: String?
    let doctorName: String?
    let analysisDate: String?
    let indicators: [AIMedicalIndicator]
}

struct AIMedicalIndicator {
    let name: String
    let value: String
    let referenceRange: String?
}

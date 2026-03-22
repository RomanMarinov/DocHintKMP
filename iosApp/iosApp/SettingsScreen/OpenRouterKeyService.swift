import Foundation

final class OpenRouterKeyService {
    static let shared = OpenRouterKeyService()
    private let baseURL = "https://openrouter.ai/"

    struct KeyInfoData: Decodable {
        let usage: Double?
        let isFreeTier: Bool?
        enum CodingKeys: String, CodingKey {
            case usage
            case isFreeTier = "is_free_tier"
        }
    }

    struct KeyResponse: Decodable {
        let data: KeyInfoData?
    }

    func getKeyInfo(apiKey: String) async throws -> (usage: Double, isFreeTier: Bool) {
        let authHeader = apiKey.hasPrefix("Bearer ") ? apiKey : "Bearer \(apiKey)"
        var request = URLRequest(url: URL(string: "\(baseURL)api/v1/key")!)
        request.setValue(authHeader, forHTTPHeaderField: "Authorization")

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw KeyError.unknown }
        if http.statusCode == 401 { throw KeyError.invalidKey }

        let decoded = try JSONDecoder().decode(KeyResponse.self, from: data)
        guard let d = decoded.data else { throw KeyError.noData }
        return (
            usage: d.usage ?? 0,
            isFreeTier: d.isFreeTier ?? true
        )
    }

    enum KeyError: LocalizedError {
        case invalidKey
        case noData
        case unknown
        var errorDescription: String? {
            switch self {
            case .invalidKey: return "Неверный API ключ"
            case .noData: return "Не удалось загрузить данные"
            case .unknown: return "Ошибка"
            }
        }
    }
}

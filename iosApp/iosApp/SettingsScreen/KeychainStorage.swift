import Foundation
import Security

final class KeychainStorage {
    private let service = "app.dochintkmp.api"
    private let key = "openrouter_api_key"

    var apiKey: String {
        get {
            var result: AnyObject?
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service,
                kSecAttrAccount as String: key,
                kSecReturnData as String: true,
                kSecMatchLimit as String: kSecMatchLimitOne
            ]
            let status = SecItemCopyMatching(query as CFDictionary, &result)
            guard status == errSecSuccess, let data = result as? Data else { return "" }
            return String(data: data, encoding: .utf8) ?? ""
        }
        set {
            if newValue.isEmpty {
                let query: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: service,
                    kSecAttrAccount as String: key
                ]
                SecItemDelete(query as CFDictionary)
            } else {
                let data = Data(newValue.utf8)
                let query: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: service,
                    kSecAttrAccount as String: key,
                    kSecValueData as String: data
                ]
                SecItemDelete(query as CFDictionary)
                SecItemAdd(query as CFDictionary, nil)
            }
        }
    }
}

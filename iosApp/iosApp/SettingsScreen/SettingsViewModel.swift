import SwiftUI

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var hasSavedKey = false
    @Published var savedKey = ""
    @Published var keyDraft = ""
    var isKeyModified: Bool { !keyDraft.isEmpty }
    @Published var keyVisible = false
    @Published var keyInfoLoading = false
    @Published var keyInfoError: String?
    @Published var keyInfo: KeyInfo?
    @Published var toastMessage: String?

    private let keychain = KeychainStorage()
    private let strings = SettingsStrings()

    struct KeyInfo {
        let usage: Double
        let isFreeTier: Bool
    }

    init() {
        hasSavedKey = !keychain.apiKey.isEmpty
        savedKey = keychain.apiKey
    }

    func loadKeyInfo() {
        let key = (hasSavedKey ? savedKey : keyDraft).trimmingCharacters(in: .whitespaces)
        if key.isEmpty { return }

        keyInfoLoading = true
        keyInfoError = nil
        Task {
            do {
                let data = try await OpenRouterKeyService.shared.getKeyInfo(apiKey: key)
                keyInfo = KeyInfo(usage: data.usage, isFreeTier: data.isFreeTier)
            } catch {
                keyInfoError = error.localizedDescription
            }
            keyInfoLoading = false
        }
    }

    func saveApiKey() {
        let key = keyDraft.trimmingCharacters(in: .whitespaces)
        if key.isEmpty { return }
        keychain.apiKey = key
        savedKey = key
        hasSavedKey = true
        keyDraft = ""
        toastMessage = strings.toastSaved
        loadKeyInfo()
    }

    func clearSavedKey() {
        keychain.apiKey = ""
        savedKey = ""
        hasSavedKey = false
        keyDraft = ""
        keyInfo = nil
        keyInfoError = nil
        toastMessage = strings.toastDeleted
    }

    func dismissToast() {
        toastMessage = nil
    }

    var maskedKey: String {
        guard savedKey.count > 8 else { return String(repeating: "•", count: savedKey.count) }
        let start = String(savedKey.prefix(4))
        let end = String(savedKey.suffix(4))
        let mid = String(repeating: "•", count: min(savedKey.count - 8, 16))
        return start + mid + end
    }
}

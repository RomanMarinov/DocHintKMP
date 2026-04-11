import SwiftUI
import Shared

enum DocumentsTab: Hashable {
    case mine
    case received
}

/// ViewModel for Documents screen.
/// Observes KMP results via DocumentListController, handles remove/clear actions.
@MainActor
final class DocumentsViewModel: ObservableObject {

    @Published var results: [DomainMedicalData] = []
    @Published var deleteIndex: Int?
    @Published var showClearDialog = false
    @Published var selectionMode = false
    @Published var selectedIndices: Set<Int> = []
    @Published var selectedTab: DocumentsTab = .mine
    @Published var inputCode = ""
    @Published var networkBusy = false
    @Published var errorMessage: String?
    @Published var importSummaryMessage: String?
    @Published var shareCode: String?
    @Published var shareExpiresAt: String?

    private let controller = DocumentListController()
    private let strings = DocumentsStrings()
    private let sourceShared = "shared"

    var resultsCountText: String {
        strings.resultsCountText(currentItems.count)
    }

    var ownItems: [DomainMedicalData] {
        results.filter { ($0.source ?? "") != sourceShared }
    }

    var receivedItems: [DomainMedicalData] {
        results.filter { ($0.source ?? "") == sourceShared }
    }

    var currentItems: [DomainMedicalData] {
        selectedTab == .mine ? ownItems : receivedItems
    }

    var selectedOwnItems: [DomainMedicalData] {
        let own = ownItems
        return selectedIndices.compactMap { idx in
            own.indices.contains(idx) ? own[idx] : nil
        }
    }

    func observeResults() {
        controller.observeResults { [weak self] list in
            self?.results = list
        }
    }

    func selectTab(_ tab: DocumentsTab) {
        selectedTab = tab
        exitSelectionMode()
    }

    func exitSelectionMode() {
        selectionMode = false
        selectedIndices = []
    }

    func enterSelectionMode() {
        selectionMode = true
        selectedIndices = []
    }

    func toggleSelection(at index: Int) {
        var next = selectedIndices
        if next.contains(index) {
            next.remove(index)
        } else {
            next.insert(index)
        }
        selectedIndices = next
    }

    func removeMineAt(visibleIndex: Int) {
        let ownIndexed = results.enumerated().filter { ($0.element.source ?? "") != sourceShared }
        guard ownIndexed.indices.contains(visibleIndex) else { return }
        controller.removeAt(index: Int32(ownIndexed[visibleIndex].offset))
        deleteIndex = nil
        exitSelectionMode()
    }

    func removeReceivedAt(visibleIndex: Int) {
        let receivedIndexed = results.enumerated().filter { ($0.element.source ?? "") == sourceShared }
        guard receivedIndexed.indices.contains(visibleIndex) else { return }
        controller.removeAt(index: Int32(receivedIndexed[visibleIndex].offset))
        deleteIndex = nil
        exitSelectionMode()
    }

    func clearResults() {
        controller.clearResults()
        showClearDialog = false
        exitSelectionMode()
    }

    func setDeleteIndex(_ index: Int?) {
        deleteIndex = index
    }

    func setShowClearDialog(_ value: Bool) {
        showClearDialog = value
    }

    func sendCode() {
        let trimmed = inputCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if trimmed.isEmpty || networkBusy { return }
        networkBusy = true
        controller.receiveDocumentsByCode(code: trimmed) { [weak self] added, skipped in
            guard let self else { return }
            self.inputCode = ""
            self.importSummaryMessage = self.strings.importSummary(added: Int(truncating: added), skipped: Int(truncating: skipped))
            self.networkBusy = false
        } onError: { [weak self] error in
            guard let self else { return }
            self.errorMessage = self.userFriendlyNetworkError(error, fallback: self.strings.importFailed)
            self.networkBusy = false
        }
    }

    func onFabTapped() {
        if selectedTab != .mine { return }
        if !selectionMode {
            enterSelectionMode()
            return
        }
        if selectedIndices.isEmpty {
            errorMessage = strings.selectAtLeastOne
            return
        }
        if networkBusy { return }
        networkBusy = true
        let items = selectedOwnItems
        controller.sendDocuments(items: items) { [weak self] code, expiresAt in
            guard let self else { return }
            self.shareCode = code
            self.shareExpiresAt = self.formatMoscowDateTime(isoUtc: expiresAt)
            self.networkBusy = false
        } onError: { [weak self] error in
            guard let self else { return }
            self.errorMessage = self.userFriendlyNetworkError(error, fallback: self.strings.shareFailed)
            self.networkBusy = false
        }
    }

    func dismissShareSheet() {
        shareCode = nil
        shareExpiresAt = nil
    }

    private func formatMoscowDateTime(isoUtc: String) -> String {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallbackIso = ISO8601DateFormatter()
        fallbackIso.formatOptions = [.withInternetDateTime]
        guard let date = iso.date(from: isoUtc) ?? fallbackIso.date(from: isoUtc) else {
            return isoUtc
        }
        let formatter = DateFormatter()
        formatter.timeZone = TimeZone(identifier: "Europe/Moscow")
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "dd.MM.yyyy HH:mm"
        return formatter.string(from: date)
    }

    private func userFriendlyNetworkError(_ raw: String?, fallback: String) -> String {
        guard let raw, !raw.isEmpty else { return fallback }
        let lowercased = raw.lowercased()
        if lowercased.contains("timeout") &&
            (lowercased.contains("localhost") || lowercased.contains("127.0.0.1")) {
            return "\(fallback)\nЕсли это симулятор, проверь что backend поднят на Mac:8081. Если реальный iPhone — задай DOCHINT_SHARE_BASE_URL (например, http://192.168.1.100:8080)."
        }
        return raw
    }
}

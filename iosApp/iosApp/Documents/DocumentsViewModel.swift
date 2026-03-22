import SwiftUI
import Shared

/// ViewModel for Documents screen.
/// Observes KMP results via DocumentListController, handles remove/clear actions.
@MainActor
final class DocumentsViewModel: ObservableObject {

    @Published var results: [DomainMedicalData] = []
    @Published var deleteIndex: Int?
    @Published var showClearDialog = false

    private let controller = DocumentListController()
    private let strings = DocumentsStrings()

    var resultsCountText: String {
        strings.resultsCountText(results.count)
    }

    func observeResults() {
        controller.observeResults { [weak self] list in
            // list от KMP уже [DomainMedicalData], приведение не требуется
            let items = list
            DispatchQueue.main.async {
                self?.results = items
            }
        }
    }

    func removeAt(index: Int) {
        controller.removeAt(index: Int32(index))
        deleteIndex = nil
    }

    func clearResults() {
        controller.clearResults()
        showClearDialog = false
    }

    func setDeleteIndex(_ index: Int?) {
        deleteIndex = index
    }

    func setShowClearDialog(_ value: Bool) {
        showClearDialog = value
    }
}

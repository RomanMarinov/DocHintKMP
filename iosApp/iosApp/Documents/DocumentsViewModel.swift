import SwiftUI
import Shared

/// ViewModel for Documents screen.
/// Observes KMP results via DocumentListController, handles remove/clear actions.
@MainActor
final class DocumentsViewModel: ObservableObject {

    @Published var results: [DomainMedicalData] = []
    @Published var deleteIndex: Int?
    @Published var showClearDialog = false
    @Published var selectionMode = false
    @Published var selectedIndices: Set<Int> = []

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

    func removeAt(index: Int) {
        controller.removeAt(index: Int32(index))
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
}

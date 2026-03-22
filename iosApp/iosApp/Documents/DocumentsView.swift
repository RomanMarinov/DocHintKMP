import SwiftUI
import Shared

/// Documents screen — UI only.
/// Business logic in DocumentsViewModel; uses shared KMP domain layer (same as Android).
struct DocumentsView: View {
    @StateObject private var viewModel = DocumentsViewModel()
    private let strings = DocumentsStrings()

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            if viewModel.results.isEmpty {
                emptyState
            } else {
                resultsList
            }
        }
        .background(Color(.systemGroupedBackground))
        .alert(strings.dialogDeleteTitle, isPresented: Binding(
            get: { viewModel.deleteIndex != nil },
            set: { if !$0 { viewModel.setDeleteIndex(nil) } }
        )) {
            Button(strings.dialogCancel, role: .cancel) { viewModel.setDeleteIndex(nil) }
            Button(strings.dialogConfirm, role: .destructive) {
                if let idx = viewModel.deleteIndex {
                    viewModel.removeAt(index: idx)
                }
            }
        } message: {
            Text(strings.dialogDeleteMessage)
        }
        .alert(strings.dialogClearTitle, isPresented: $viewModel.showClearDialog) {
            Button(strings.dialogCancel, role: .cancel) { viewModel.setShowClearDialog(false) }
            Button(strings.dialogConfirm, role: .destructive) {
                viewModel.clearResults()
            }
        } message: {
            Text(strings.dialogClearMessage)
        }
        .onAppear {
            viewModel.observeResults()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(strings.screenDocuments)
                .font(.largeTitle)
                .fontWeight(.bold)
            HStack {
                Text(viewModel.results.isEmpty ? strings.noSavedAnalyses : viewModel.resultsCountText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                if !viewModel.results.isEmpty {
                    Button(strings.allClearData) {
                        viewModel.setShowClearDialog(true)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .padding(.bottom, 16)
        .background(Color(.systemBackground))
    }

    private var emptyState: some View {
        VStack(spacing: 24) {
            RoundedRectangle(cornerRadius: 28)
                .fill(Color(.secondarySystemBackground))
                .frame(width: 96, height: 96)
                .overlay {
                    Image(systemName: "folder.open")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary)
                }
            Text(strings.emptyTitle)
                .font(.title2)
                .fontWeight(.semibold)
            Text(strings.emptySubtitle)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(32)
    }

    private var resultsList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(Array(viewModel.results.enumerated()), id: \.offset) { index, data in
                    DocumentsResultCard(data: data) {
                        viewModel.setDeleteIndex(index)
                    }
                }
            }
            .padding(16)
        }
    }
}

// MARK: - Preview

struct DocumentsView_Previews: PreviewProvider {
    static var previews: some View {
        DocumentsView()
    }
}

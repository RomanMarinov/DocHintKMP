import SwiftUI
import Shared

/// Documents screen — UI only.
/// Business logic in DocumentsViewModel; uses shared KMP domain layer (same as Android).
struct DocumentsScreen: View {
    @StateObject private var viewModel = DocumentsViewModel()
    private let strings = DocumentsStrings()

    /// Соответствует Android `DocumentsListBottomInsetFab` (список не уезжает под FAB).
    private let listBottomInsetFab: CGFloat = 88

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                topBar
                if viewModel.results.isEmpty {
                    emptyState
                } else {
                    resultsList
                }
            }
            if !viewModel.results.isEmpty {
                documentsFab
                    .padding(.trailing, 16)
                    .padding(.bottom, 16)
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

    private var documentsSubtitle: String {
        if viewModel.results.isEmpty {
            strings.noSavedAnalyses
        } else if viewModel.selectionMode {
            strings.selectedCount(viewModel.selectedIndices.count)
        } else {
            viewModel.resultsCountText
        }
    }

    private var topBar: some View {
        AppTopBar(title: strings.screenDocuments, subtitle: documentsSubtitle) {
            if !viewModel.results.isEmpty {
                if viewModel.selectionMode {
                    Button(strings.selectionCancel) {
                        viewModel.exitSelectionMode()
                    }
                    .font(.body)
                    .buttonStyle(.plain)
                    .foregroundStyle(Color.accentColor)
                } else {
                    Button(strings.allClearData) {
                        viewModel.setShowClearDialog(true)
                    }
                    .font(.body)
                    .buttonStyle(.plain)
                    .foregroundStyle(.red)
                }
            }
        }
    }

    private var fabPickPhase: Bool {
        viewModel.selectionMode && viewModel.selectedIndices.isEmpty
    }

    private var fabTitle: String {
        if !viewModel.selectionMode { return strings.actionSend }
        if fabPickPhase { return strings.fabPickFirst }
        return "\(strings.share) (\(viewModel.selectedIndices.count))"
    }

    private var documentsFab: some View {
        Button {
            if !viewModel.selectionMode {
                viewModel.enterSelectionMode()
            } else if !viewModel.selectedIndices.isEmpty {
                // Экспорт / share sheet — позже, как на Android.
            }
        } label: {
            Label(fabTitle, systemImage: "square.and.arrow.up")
                .labelStyle(.titleAndIcon)
                .font(AppProminentActionMetrics.labelFont)
                .padding(.horizontal, AppProminentActionMetrics.labelHorizontalPadding)
                .padding(.vertical, AppProminentActionMetrics.labelVerticalPadding)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .controlSize(.regular)
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
                    DocumentsResultCard(
                        data: data,
                        selectionMode: viewModel.selectionMode,
                        selected: viewModel.selectedIndices.contains(index),
                        onToggleSelect: { viewModel.toggleSelection(at: index) },
                        onDelete: { viewModel.setDeleteIndex(index) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, listBottomInsetFab)
        }
    }
}

// MARK: - Preview

struct DocumentsView_Previews: PreviewProvider {
    static var previews: some View {
        DocumentsScreen()
    }
}

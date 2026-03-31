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
                tabBar
                if viewModel.selectedTab == .received {
                    receivedTabContent
                } else {
                    if viewModel.currentItems.isEmpty {
                        emptyState(text: strings.noSavedAnalyses)
                    } else {
                        resultsList
                    }
                }
            }
            if viewModel.selectedTab == .mine && !viewModel.ownItems.isEmpty {
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
                    if viewModel.selectedTab == .mine {
                        viewModel.removeMineAt(visibleIndex: idx)
                    } else {
                        viewModel.removeReceivedAt(visibleIndex: idx)
                    }
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
        .alert(strings.dialogErrorTitle, isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button(strings.dialogOk, role: .cancel) { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .alert(strings.dialogImportResultTitle, isPresented: Binding(
            get: { viewModel.importSummaryMessage != nil },
            set: { if !$0 { viewModel.importSummaryMessage = nil } }
        )) {
            Button(strings.dialogOk, role: .cancel) { viewModel.importSummaryMessage = nil }
        } message: {
            Text(viewModel.importSummaryMessage ?? "")
        }
        .sheet(isPresented: Binding(
            get: { viewModel.shareCode != nil },
            set: { if !$0 { viewModel.dismissShareSheet() } }
        )) {
            shareSheet
                .presentationDetents([.medium])
        }
        .onAppear {
            viewModel.observeResults()
        }
    }

    private var documentsSubtitle: String {
        if viewModel.currentItems.isEmpty {
            viewModel.selectedTab == .mine ? strings.noSavedAnalyses : strings.noImported
        } else if viewModel.selectionMode {
            strings.selectedCount(viewModel.selectedIndices.count)
        } else {
            viewModel.resultsCountText
        }
    }

    private var topBar: some View {
        AppTopBar(title: strings.screenDocuments, subtitle: documentsSubtitle) {
            if viewModel.selectedTab == .mine && !viewModel.ownItems.isEmpty {
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
            viewModel.onFabTapped()
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

    private var tabBar: some View {
        Picker("", selection: Binding(
            get: { viewModel.selectedTab },
            set: { viewModel.selectTab($0) }
        )) {
            Text(strings.tabMine).tag(DocumentsTab.mine)
            Text(strings.tabReceived).tag(DocumentsTab.received)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private func emptyState(text: String) -> some View {
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
            Text(text)
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
                ForEach(Array(viewModel.currentItems.enumerated()), id: \.offset) { index, data in
                    DocumentsResultCard(
                        data: data,
                        selectionMode: viewModel.selectionMode && viewModel.selectedTab == .mine,
                        selected: viewModel.selectedIndices.contains(index),
                        onToggleSelect: {
                            if viewModel.selectedTab == .mine {
                                viewModel.toggleSelection(at: index)
                            }
                        },
                        onDelete: { viewModel.setDeleteIndex(index) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, viewModel.selectedTab == .mine ? listBottomInsetFab : 16)
        }
    }

    private var receivedTabContent: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                TextField("Вставьте одноразовый код", text: $viewModel.inputCode)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .onSubmit { viewModel.sendCode() }

                Button {
                    viewModel.sendCode()
                } label: {
                    Image(systemName: "square.and.arrow.down")
                        .foregroundStyle(
                            viewModel.inputCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                ? .secondary
                                : Color.accentColor
                        )
                }
                .buttonStyle(.plain)
                .disabled(viewModel.inputCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.networkBusy)
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(Color(.secondarySystemBackground))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(.separator), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .padding(.top, 8)

            if viewModel.currentItems.isEmpty {
                emptyState(text: strings.noImported)
            } else {
                resultsList
            }
        }
    }

    private var shareSheet: some View {
        VStack(spacing: 12) {
            Text(strings.shareCodeTitle)
                .font(.title2)
                .fontWeight(.semibold)

            Text(viewModel.shareCode ?? "")
                .font(.largeTitle.monospacedDigit())
                .fontWeight(.bold)

            if let expiresAt = viewModel.shareExpiresAt {
                Text(strings.shareValidUntil(expiresAt))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Button(strings.copyCode) {
                UIPasteboard.general.string = viewModel.shareCode
                viewModel.dismissShareSheet()
            }
            .buttonStyle(.bordered)
        }
        .padding(20)
    }
}

// MARK: - Preview

struct DocumentsView_Previews: PreviewProvider {
    static var previews: some View {
        DocumentsScreen()
    }
}

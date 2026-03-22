import SwiftUI

/// AI Scanner screen — design matches Android OcrAiScannerScreen.
struct AIScannerScreen: View {
    @StateObject private var viewModel = AIScannerViewModel()
    @State private var resultExpanded = false
    @State private var showFilePicker = false
    private let strings = AIScannerStrings()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                headerView
                contentSection
            }
        }
        .background(Color(.systemGroupedBackground))
        .onAppear { viewModel.refreshKey() }
        .fileImporter(
            isPresented: $showFilePicker,
            allowedContentTypes: viewModel.supportedTypes,
            allowsMultipleSelection: false
        ) { result in
            viewModel.handleFilePick(result: result)
        }
        .overlay { toastOverlay }
    }

    private var headerView: some View {
        Text(strings.screenTitle)
            .font(.largeTitle)
            .fontWeight(.bold)
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 8)
    }

    @ViewBuilder
    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !viewModel.hasSavedKey {
                apiKeyHint
            }
            if viewModel.selectedImage == nil {
                selectImageCard
            } else {
                selectedImageSection
            }
            if case .loading = viewModel.contentState, viewModel.selectedImage != nil {
                AILoadingCard()
            }
            if case .error(let msg) = viewModel.contentState {
                AIErrorCard(message: msg)
            }
        }
        .padding(.horizontal, 16)
    }

    private var apiKeyHint: some View {
        Text(strings.addApiKeyHint)
            .font(.subheadline)
            .foregroundStyle(.primary)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemRed).opacity(0.2))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var selectImageCard: some View {
        Button(action: { showFilePicker = true }) {
            VStack(spacing: 12) {
                Image(systemName: "doc.badge.plus")
                    .font(.system(size: 40))
                    .foregroundStyle(Color.accentColor)
                Text(strings.selectImage)
                    .font(.headline)
                    .foregroundStyle(Color.accentColor)
                Text(strings.supportedFormats)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 160)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color(.systemGray4), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var selectedImageSection: some View {
        VStack(spacing: 16) {
            imagePreviewWithClose
            if case .success(let data) = viewModel.contentState {
                expandableResultCard(data: data)
            } else {
                recognizeButton
            }
        }
    }

    private var imagePreviewWithClose: some View {
        ZStack(alignment: .topTrailing) {
            if let img = viewModel.selectedImage {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 220)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            Button(action: { viewModel.resetState() }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title2)
                    .foregroundStyle(.secondary)
            }
            .padding(12)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }

    private func expandableResultCard(data: AIMedicalData) -> some View {
        VStack(spacing: 0) {
            Button(action: { withAnimation { resultExpanded.toggle() } }) {
                HStack {
                    Text(strings.viewResult)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Spacer()
                    Image(systemName: resultExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
            }
            .buttonStyle(.plain)
            if resultExpanded {
                Divider()
                AISuccessCard(data: data, inDropdown: true)
            }
        }
        .background(Color(.secondarySystemBackground).opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var recognizeButton: some View {
        Button(action: { Task { await viewModel.processImage() } }) {
            HStack(spacing: 8) {
                if case .loading = viewModel.contentState {
                    ProgressView().tint(.white)
                    Text(strings.processing)
                } else {
                    Image(systemName: "stethoscope")
                    Text(strings.recognizeDocument)
                }
            }
            .fontWeight(.semibold)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
        }
        .buttonStyle(.borderedProminent)
        .disabled(!viewModel.hasSavedKey || viewModel.contentState.isLoading)
    }

    @ViewBuilder
    private var toastOverlay: some View {
        if let msg = viewModel.toastMessage {
            AIToastView(message: msg)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        withAnimation { viewModel.dismissToast() }
                    }
                }
        }
    }
}

extension AIScannerViewModel.AIContentState {
    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }
}

#Preview {
    AIScannerScreen()
}

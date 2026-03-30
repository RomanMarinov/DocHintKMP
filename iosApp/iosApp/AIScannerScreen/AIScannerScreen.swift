import SwiftUI

/// AI Scanner screen — design matches Android OcrAiScannerScreen.
struct AIScannerScreen: View {
    @StateObject private var viewModel = AIScannerViewModel()
    @State private var showFilePicker = false
    private let strings = AIScannerStrings()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                topBar
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
        .overlay(alignment: .bottom) { toastOverlay }
    }

    private var topBar: some View {
        AppTopBar(title: strings.screenTitle)
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
            if case .error(let msg) = viewModel.contentState {
                AIErrorCard(message: msg)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 16)
    }

    private var apiKeyHint: some View {
        Text(strings.addApiKeyHint)
            .font(.subheadline)
            .foregroundStyle(Color.primary)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemRed).opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var selectImageCard: some View {
        Button(action: { showFilePicker = true }) {
            VStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor.opacity(0.2))
                        .frame(width: 56, height: 56)
                    Image(systemName: "doc.badge.plus")
                        .font(.system(size: 28))
                        .foregroundStyle(Color.accentColor)
                }
                Text(strings.selectImage)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.accentColor)
                Text(strings.supportedFormats)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 160)
            .background(Color(.systemBackground))
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
            if case .success(let data, _) = viewModel.contentState {
                AIExpandableResultCard(data: data)
                Button(action: { viewModel.saveDocument() }) {
                    Text(strings.saveToDocuments)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
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
            CloseButton(action: { viewModel.resetState() })
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
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
            .font(AppProminentActionMetrics.labelFont)
            .fontWeight(.semibold)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, AppProminentActionMetrics.labelHorizontalPadding)
            .padding(.vertical, AppProminentActionMetrics.labelVerticalPadding)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.roundedRectangle(radius: 16))
        .controlSize(.regular)
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

import SwiftUI
import UniformTypeIdentifiers

struct OfflineScannerScreen: View {
    @StateObject private var viewModel = OfflineScannerViewModel()
    private let strings = OfflineScannerStrings()

    private var topBar: some View {
        AppTopBar(title: strings.screenTitle)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                topBar

                VStack(alignment: .leading, spacing: 16) {
                    documentCard
                        .padding(.horizontal, 16)

                    if viewModel.selectedURL == nil {
                        Text(strings.instructions)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 24)
                            .padding(.bottom, 24)
                    }
                }
                .padding(.top, 16)
            }
        }
        .background(Color(.systemGroupedBackground))
        .fileImporter(
            isPresented: $viewModel.showFilePicker,
            allowedContentTypes: viewModel.supportedTypes,
            allowsMultipleSelection: false
        ) { result in
            viewModel.handleFilePick(result: result)
        }
        .overlay(alignment: .bottom) {
            if let msg = viewModel.toastMessage {
                OfflineToastView(message: msg)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation { viewModel.dismissToast() }
                        }
                    }
            }
        }
    }

    private var documentCard: some View {
        Group {
            if viewModel.selectedURL == nil {
                selectFileCard
            } else {
                selectedFileCard
            }
        }
    }

    private var selectFileCard: some View {
        Button(action: { viewModel.showFilePicker = true }) {
            VStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor.opacity(0.2))
                        .frame(width: 56, height: 56)
                    Image(systemName: "doc.badge.plus")
                        .font(.system(size: 28))
                        .foregroundStyle(Color.accentColor)
                }
                Text(strings.selectFile)
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

    private var selectedFileCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Card: only preview + close
            ZStack(alignment: .topTrailing) {
                previewView
                CloseButton(action: { viewModel.resetState() })
            }
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: .black.opacity(0.06), radius: 8, y: 2)

            // Buttons outside card (like AI Scanner)
            if case .idle = viewModel.contentState {
                recognizeButton
            }
            if case .loading = viewModel.contentState {
                recognizeButton
            }
            if case .success(let data, let processedText) = viewModel.contentState {
                ExpandableResultCard(data: data)
                Button(action: { viewModel.saveDocument(data: data, processedText: processedText) }) {
                    Text(strings.saveToDocuments)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            if case .error(let message) = viewModel.contentState {
                OfflineErrorCard(message: message)
            }
        }
    }

    private var recognizeButton: some View {
        Button(action: {
            Task { await viewModel.processFile() }
        }) {
            HStack(spacing: 8) {
                if case .loading = viewModel.contentState {
                    ProgressView().tint(.white)
                    Text(strings.processing)
                } else {
                    Text(strings.recognize)
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
        .disabled(viewModel.contentState.isLoading)
    }

    private var previewView: some View {
        Group {
            if viewModel.fileType.isImage, let url = viewModel.selectedURL, let img = viewModel.loadImage(from: url) {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 200)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else if viewModel.fileType == .pdf, let img = viewModel.previewImage {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 200)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                fileTypePlaceholder
            }
        }
    }

    private var fileTypePlaceholder: some View {
        OfflineFileTypePlaceholder(fileType: viewModel.fileType)
    }

}

extension OfflineContentState {
    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }
}

struct OfflineScannerView_Previews: PreviewProvider {
    static var previews: some View {
        OfflineScannerScreen()
    }
}

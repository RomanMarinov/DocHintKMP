import SwiftUI
import UniformTypeIdentifiers

struct OfflineScannerScreen: View {
    @StateObject private var viewModel = OfflineScannerViewModel()
    private let strings = OfflineScannerStrings()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(strings.screenTitle)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .padding(.horizontal, 24)
                        .padding(.top, 24)

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
            }
            .background(Color(.systemGroupedBackground))
            .fileImporter(
                isPresented: $viewModel.showFilePicker,
                allowedContentTypes: viewModel.supportedTypes,
                allowsMultipleSelection: false
            ) { result in
                viewModel.handleFilePick(result: result)
            }
            .overlay {
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
                Image(systemName: "doc.badge.plus")
                    .font(.system(size: 40))
                    .foregroundStyle(.tint)
                Text(strings.selectFile)
                    .font(.headline)
                Text(strings.supportedFormats)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 160)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private var selectedFileCard: some View {
        VStack(spacing: 16) {
            ZStack(alignment: .topTrailing) {
                previewView
                Button(action: { viewModel.resetState() }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                }
                .padding(12)
            }

            if case .idle = viewModel.contentState {
                Button(action: {
                    Task { await viewModel.processFile() }
                }) {
                    Text(strings.recognize)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
            }

            if case .loading = viewModel.contentState {
                OfflineLoadingView()
            }

            if case .success(let data) = viewModel.contentState {
                ExpandableResultCard(data: data)
                Button(action: { viewModel.saveDocument(data: data) }) {
                    Text(strings.saveToDocuments)
                        .fontWeight(.semibold)
                        .frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
            }

            if case .error(let message) = viewModel.contentState {
                OfflineErrorCard(message: message)
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
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

struct OfflineScannerView_Previews: PreviewProvider {
    static var previews: some View {
        OfflineScannerScreen()
    }
}

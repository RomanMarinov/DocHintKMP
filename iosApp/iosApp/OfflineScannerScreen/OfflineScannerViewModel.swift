import SwiftUI
import UniformTypeIdentifiers
import Shared

@MainActor
final class OfflineScannerViewModel: ObservableObject {

    @Published var selectedURL: URL?
    @Published var fileType: OfflineFileType = .pdf
    @Published var contentState: OfflineContentState = .idle
    @Published var showFilePicker = false
    @Published var toastMessage: String?
    @Published var previewImage: UIImage?

    private let controller = OfflineScannerController()
    private let strings = OfflineScannerStrings()

    var supportedTypes: [UTType] {
        [.image, .jpeg, .png, .pdf, .plainText] + [UTType(filenameExtension: "docx")].compactMap { $0 }
    }

    func handleFilePick(result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            _ = url.startAccessingSecurityScopedResource()
            selectedURL = url
            fileType = OfflineFileType(
                uti: url.pathExtension == "pdf" ? UTType.pdf.identifier :
                     url.pathExtension == "txt" ? UTType.plainText.identifier :
                     url.pathExtension == "docx" ? "org.openxmlformats.wordprocessingml.document" :
                     url.pathExtension == "png" ? UTType.png.identifier :
                     UTType.jpeg.identifier
            ) ?? .image
            contentState = .idle
            if fileType == .pdf {
                Task { await loadPdfPreview(url: url) }
            }
        case .failure:
            toastMessage = strings.toastFileOpenFailed
        }
    }

    private func loadPdfPreview(url: URL) async {
        let img = await OfflineScannerTextExtractor.loadPdfPreview(url: url)
        previewImage = img
    }

    func loadImage(from url: URL) -> UIImage? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }

    func processFile() async {
        guard let url = selectedURL else { return }
        if fileType == .docx {
            contentState = .error(strings.errorDocxUnsupported)
            return
        }
        contentState = .loading

        let currentFileType = fileType
        let rawText = await Task.detached(priority: .userInitiated) {
            OfflineScannerTextExtractor.extract(from: url, fileType: currentFileType)
        }.value

        guard let text = rawText, !text.isEmpty else {
            contentState = .error(strings.errorExtractFailed)
            return
        }

        let result = controller.processText(rawText: text)

        if let data = result.data, let processedText = result.processedText {
            contentState = .success(data, processedText: processedText)
        } else if let errorType = result.errorType {
            let msg: String
            switch errorType {
            case .emptyText: msg = strings.errorEmptyText
            case .tooShort: msg = strings.errorTooShort
            case .noMedicalData: msg = strings.errorNoMedicalData
            case .duplicateDocument: msg = strings.errorDuplicate
            case .parseFailed: msg = strings.errorParseFailed
            default: msg = strings.errorUnknown
            }
            contentState = .error(msg)
        } else {
            contentState = .error(strings.errorUnknown)
        }
    }

    func saveDocument(data: DomainMedicalData, processedText: String) {
        controller.saveDocument(data: data, processedText: processedText)
        toastMessage = strings.toastAddedToDocuments
        resetState()
    }

    func resetState() {
        selectedURL?.stopAccessingSecurityScopedResource()
        selectedURL = nil
        previewImage = nil
        contentState = .idle
    }

    func dismissToast() {
        toastMessage = nil
    }
}

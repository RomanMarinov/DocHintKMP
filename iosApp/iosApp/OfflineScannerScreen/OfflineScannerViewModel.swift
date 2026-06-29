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

    private let textExtractor: OfflineScannerTextExtracting
    private let scanning: OfflineScanningBackend
    private let strings: OfflineScannerStrings

    init(
        textExtractor: OfflineScannerTextExtracting = DefaultOfflineScannerTextExtractor(),
        scanning: OfflineScanningBackend = KotlinOfflineScanningBackend(),
        strings: OfflineScannerStrings = OfflineScannerStrings()
    ) {
        self.textExtractor = textExtractor
        self.scanning = scanning
        self.strings = strings
    }

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
        let img = await textExtractor.loadPdfPreview(url: url)
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
        let rawText = await textExtractor.extract(from: url, fileType: currentFileType)

        guard let text = rawText, !text.isEmpty else {
            contentState = .error(strings.errorExtractFailed)
            return
        }

        let result = scanning.processText(rawText: text)

        if let data = result.data, let processedText = result.processedText {
            contentState = .success(data, processedText: processedText)
        } else if let err = result.error {
            let msg: String
            switch err {
            case .emptyText: msg = strings.errorEmptyText
            case .tooShort: msg = strings.errorTooShort
            case .noHousingBill: msg = strings.errorNoHousingBill
            case .duplicateDocument: msg = strings.errorDuplicate
            case .parseFailed: msg = strings.errorParseFailed
            case .unknown: msg = strings.errorUnknown
            }
            contentState = .error(msg)
        } else {
            contentState = .error(strings.errorUnknown)
        }
    }

    func saveDocument(data: DomainHousingPaymentDocument, processedText: String) {
        scanning.saveDocument(data: data, processedText: processedText)
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

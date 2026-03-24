import SwiftUI
import UniformTypeIdentifiers
import Vision
import PDFKit
import Shared

@MainActor
final class AIScannerViewModel: ObservableObject {
    @Published var selectedImage: UIImage?
    @Published var selectedURL: URL?
    @Published var contentState: AIContentState = .idle
    @Published var hasSavedKey = false
    @Published var toastMessage: String?
    private let strings = AIScannerStrings()
    private let keychain = KeychainStorage()
    private let openRouter = OpenRouterService()
    private let aiController = AIScannerController()

    var supportedTypes: [UTType] {
        [.image, .jpeg, .png, .pdf]
    }

    enum AIContentState {
        case idle
        case loading
        /// Parsed result + OCR text; save to documents is explicit via `saveDocument()` (same flow as offline scanner).
        case success(AIMedicalData, processedText: String)
        case error(String)
    }

    init() {
        hasSavedKey = !keychain.apiKey.isEmpty
    }

    func refreshKey() {
        hasSavedKey = !keychain.apiKey.isEmpty
    }

    func handleFilePick(result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            _ = url.startAccessingSecurityScopedResource()
            selectedURL = url
            let ext = url.pathExtension.lowercased()
            if ext == "pdf" {
                selectedImage = loadPdfPreview(from: url)
            } else {
                selectedImage = UIImage(contentsOfFile: url.path)
                    ?? (try? Data(contentsOf: url)).flatMap { UIImage(data: $0) }
            }
            contentState = .idle
        case .failure:
            toastMessage = strings.errorUnknown
        }
    }

    private func loadPdfPreview(from url: URL) -> UIImage? {
        guard let doc = PDFDocument(url: url),
              let page = doc.page(at: 0) else { return nil }
        let rect = page.bounds(for: .mediaBox)
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: rect.width, height: rect.height))
        return renderer.image { ctx in
            ctx.cgContext.translateBy(x: 0, y: rect.height)
            ctx.cgContext.scaleBy(x: 1, y: -1)
            page.draw(with: .mediaBox, to: ctx.cgContext)
        }
    }

    func resetState() {
        selectedURL?.stopAccessingSecurityScopedResource()
        selectedURL = nil
        selectedImage = nil
        contentState = .idle
    }

    func processImage() async {
        guard let img = selectedImage else {
            showToast(strings.errorSelectImage)
            return
        }
        let key = keychain.apiKey.trimmingCharacters(in: .whitespaces)
        if key.isEmpty {
            showToast(strings.errorSaveKey)
            return
        }

        contentState = .loading
        defer { }

        do {
            let cleanText: String
            if let url = selectedURL, url.pathExtension.lowercased() == "pdf" {
                cleanText = OfflineScannerTextExtractor.extract(from: url, fileType: .pdf) ?? ""
            } else {
                cleanText = try extractText(from: img)
            }
            if cleanText.isEmpty {
                contentState = .error(strings.errorUnknown)
                return
            }

            if aiController.checkDuplicate(cleanText: cleanText) {
                contentState = .error(strings.errorDuplicate)
                return
            }

            let result = try await openRouter.parseWithLlm(apiKey: key, cleanText: cleanText)
            contentState = .success(result, processedText: cleanText)
        } catch let e as OpenRouterService.AIError {
            switch e {
            case .invalidKey: contentState = .error(strings.errorSaveKey)
            case .noMedicalData: contentState = .error(e.localizedDescription)
            case .apiError(let m): contentState = .error(m)
            default: contentState = .error(e.localizedDescription)
            }
        } catch let e as URLError where e.code == .notConnectedToInternet {
            contentState = .error(strings.errorNoNetwork)
        } catch {
            contentState = .error(strings.errorUnknown)
        }
    }

    func dismissToast() {
        toastMessage = nil
    }

    func saveDocument() {
        guard case let .success(data, processedText) = contentState else { return }
        let domainData = toDomainMedicalData(data)
        aiController.saveDocument(data: domainData, processedText: processedText)
        showToast(strings.toastAddedToDocuments)
        resetState()
    }

    private func extractText(from image: UIImage) throws -> String {
        guard let cgImage = image.cgImage else { return "" }
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = false

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try handler.perform([request])

        return (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
    }

    private func toDomainMedicalData(_ d: AIMedicalData) -> DomainMedicalData {
        let indicators = d.indicators.map { ind in
            DomainAnalysisIndicator(
                name: ind.name,
                value: ind.value,
                referenceRange: ind.referenceRange
            )
        }
        return DomainMedicalData(
            documentType: d.documentType,
            institution: d.institution,
            doctorName: d.doctorName,
            analysisDate: d.analysisDate,
            indicators: indicators
        )
    }

    private func showToast(_ msg: String) {
        toastMessage = msg
    }
}

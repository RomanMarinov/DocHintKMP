import SwiftUI
import PhotosUI
import Vision
import Shared

@MainActor
final class AIScannerViewModel: ObservableObject {
    @Published var selectedImage: UIImage?
    @Published var contentState: AIContentState = .idle
    @Published var hasSavedKey = false
    @Published var toastMessage: String?
    private let strings = AIScannerStrings()
    private let keychain = KeychainStorage()
    private let openRouter = OpenRouterService()
    private let aiController = AIScannerController()

    enum AIContentState {
        case idle
        case loading
        case success(AIMedicalData)
        case error(String)
    }

    init() {
        hasSavedKey = !keychain.apiKey.isEmpty
    }

    func refreshKey() {
        hasSavedKey = !keychain.apiKey.isEmpty
    }

    func loadImage(from item: PhotosPickerItem?) async {
        guard let item else {
            selectedImage = nil
            contentState = .idle
            return
        }
        if let data = try? await item.loadTransferable(type: Data.self),
           let img = UIImage(data: data) {
            selectedImage = img
            contentState = .idle
        }
    }

    func resetState() {
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
            let cleanText = try extractText(from: img)
            if cleanText.isEmpty {
                contentState = .error(strings.errorUnknown)
                return
            }

            if aiController.checkDuplicate(cleanText: cleanText) {
                contentState = .error(strings.errorDuplicate)
                return
            }

            let result = try await openRouter.parseWithLlm(apiKey: key, cleanText: cleanText)
            contentState = .success(result)

            let domainData = toDomainMedicalData(result)
            aiController.saveDocument(data: domainData)
            showToast(strings.toastAddedToDocuments)
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

import Foundation
import UIKit
import Vision
import Shared

protocol AIScannerKeychainReading: AnyObject {
    var apiKey: String { get }
}

extension KeychainStorage: AIScannerKeychainReading {}

protocol AIScannerLlmClient: AnyObject {
    func parseWithLlm(apiKey: String, cleanText: String) async throws -> AIMedicalData
}

extension OpenRouterService: AIScannerLlmClient {}

/// Duplicate check + save via KMP [AIScannerController].
protocol AIScannerDomainBackend: AnyObject {
    func checkDuplicate(cleanText: String) -> Bool
    func saveDocument(data: DomainMedicalData, processedText: String)
}

final class KotlinAIScannerDomainBackend: AIScannerDomainBackend {
    private let controller: AIScannerController

    init(controller: AIScannerController = AIScannerController()) {
        self.controller = controller
    }

    func checkDuplicate(cleanText: String) -> Bool {
        controller.checkDuplicate(cleanText: cleanText)
    }

    func saveDocument(data: DomainMedicalData, processedText: String) {
        controller.saveDocument(data: data, processedText: processedText)
    }
}

/// PDF text via [OfflineScannerTextExtractor]; image text via Vision (same as previous VM).
protocol AIScannerOcrProviding: AnyObject {
    func extractPdfText(from url: URL) async -> String?
    func extractText(from image: UIImage) throws -> String
}

final class DefaultAIScannerOcrProvider: AIScannerOcrProviding {
    func extractPdfText(from url: URL) async -> String? {
        await Task(priority: .userInitiated) {
            OfflineScannerTextExtractor.extract(from: url, fileType: .pdf)
        }.value
    }

    func extractText(from image: UIImage) throws -> String {
        guard let cgImage = image.cgImage else { return "" }
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = false

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try handler.perform([request])

        return (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
    }
}

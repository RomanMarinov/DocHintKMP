import Foundation
import UIKit
import Shared

enum OfflineScanProcessError: Equatable {
    case emptyText
    case tooShort
    case noHousingBill
    case duplicateDocument
    case parseFailed
    case unknown
}

struct OfflineScanProcessResult {
    var data: DomainHousingPaymentDocument?
    var processedText: String?
    var error: OfflineScanProcessError?
}

/// Text extraction for offline scanner (images, PDF, txt). Fakes enable unit tests without Vision/PDFKit files.
protocol OfflineScannerTextExtracting: AnyObject {
    func extract(from url: URL, fileType: OfflineFileType) async -> String?
    func loadPdfPreview(url: URL) async -> UIImage?
}

final class DefaultOfflineScannerTextExtractor: OfflineScannerTextExtracting {
    func extract(from url: URL, fileType: OfflineFileType) async -> String? {
        await Task(priority: .userInitiated) {
            OfflineScannerTextExtractor.extract(from: url, fileType: fileType)
        }.value
    }

    func loadPdfPreview(url: URL) async -> UIImage? {
        await OfflineScannerTextExtractor.loadPdfPreview(url: url)
    }
}

/// Parse, duplicate check, save — mirrors KMP [OfflineScannerController] for production; fakes for tests.
protocol OfflineScanningBackend: AnyObject {
    func processText(rawText: String) -> OfflineScanProcessResult
    func saveDocument(data: DomainHousingPaymentDocument, processedText: String)
}

final class KotlinOfflineScanningBackend: OfflineScanningBackend {
    private let controller: OfflineScannerController

    init(controller: OfflineScannerController = OfflineScannerController()) {
        self.controller = controller
    }

    func processText(rawText: String) -> OfflineScanProcessResult {
        let r = controller.processText(rawText: rawText)
        if let data = r.data, let text = r.processedText {
            return OfflineScanProcessResult(data: data, processedText: text, error: nil)
        }
        if let e = r.errorType {
            return OfflineScanProcessResult(data: nil, processedText: nil, error: mapError(e))
        }
        return OfflineScanProcessResult(data: nil, processedText: nil, error: .unknown)
    }

    func saveDocument(data: DomainHousingPaymentDocument, processedText: String) {
        controller.saveDocument(data: data, processedText: processedText)
    }

    private func mapError(_ e: OfflineScannerController.ErrorType) -> OfflineScanProcessError {
        switch e {
        case .emptyText: return .emptyText
        case .tooShort: return .tooShort
        case .noHousingBill: return .noHousingBill
        case .duplicateDocument: return .duplicateDocument
        case .parseFailed: return .parseFailed
        default: return .unknown
        }
    }
}

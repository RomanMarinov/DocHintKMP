import Foundation
import UIKit
import Vision
import PDFKit

/// iOS-native text extraction from images and PDFs.
/// Separated from View/ViewModel for single responsibility.
enum OfflineScannerTextExtractor {

    static func extract(from url: URL, fileType: OfflineFileType) -> String? {
        switch fileType {
        case .image, .png:
            return extractTextFromImage(url: url)
        case .pdf:
            return extractTextFromPdf(url: url)
        case .txt:
            return try? String(contentsOf: url, encoding: .utf8)
        case .docx:
            return nil // DOCX not supported on iOS
        }
    }

    static func loadPdfPreview(url: URL) async -> UIImage? {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                guard let doc = PDFDocument(url: url),
                      let page = doc.page(at: 0) else {
                    continuation.resume(returning: nil)
                    return
                }
                let rect = page.bounds(for: .mediaBox)
                let renderer = UIGraphicsImageRenderer(size: CGSize(width: rect.width, height: rect.height))
                let img = renderer.image { ctx in
                    ctx.cgContext.translateBy(x: 0, y: rect.height)
                    ctx.cgContext.scaleBy(x: 1, y: -1)
                    page.draw(with: .mediaBox, to: ctx.cgContext)
                }
                continuation.resume(returning: img)
            }
        }
    }

    private static func extractTextFromImage(url: URL) -> String? {
        guard let data = try? Data(contentsOf: url),
              let img = UIImage(data: data),
              let cgImage = img.cgImage else { return nil }
        return recognizeText(in: cgImage)
    }

    private static func extractTextFromPdf(url: URL) -> String? {
        guard let doc = PDFDocument(url: url) else { return nil }
        var text = doc.string ?? ""
        if text.isEmpty {
            for i in 0..<doc.pageCount {
                guard let page = doc.page(at: i) else { continue }
                let pageRect = page.bounds(for: .mediaBox)
                guard let pageImage = renderPdfPage(page, size: pageRect.size),
                      let cgImage = pageImage.cgImage else { continue }
                if let pageText = recognizeText(in: cgImage) {
                    text += pageText + "\n"
                }
            }
        }
        return text.isEmpty ? nil : text
    }

    private static func renderPdfPage(_ page: PDFPage, size: CGSize) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            ctx.cgContext.translateBy(x: 0, y: size.height)
            ctx.cgContext.scaleBy(x: 1, y: -1)
            page.draw(with: .mediaBox, to: ctx.cgContext)
        }
    }

    private static func recognizeText(in cgImage: CGImage) -> String? {
        var result: String?
        let request = VNRecognizeTextRequest { request, _ in
            let observations = request.results as? [VNRecognizedTextObservation] ?? []
            result = observations.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
        }
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["ru-RU", "en-US"]
        request.usesLanguageCorrection = true

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try? handler.perform([request])
        return result
    }
}

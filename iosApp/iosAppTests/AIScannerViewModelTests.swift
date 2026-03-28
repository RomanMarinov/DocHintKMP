import XCTest
import UIKit
import Shared
@testable import DocHint

@MainActor
final class AIScannerViewModelTests: XCTestCase {

    func testProcessImage_withoutImage_showsSelectImageToast() async {
        let strings = AIScannerStrings()
        let vm = AIScannerViewModel(
            strings: strings,
            keychain: FakeKeychain(apiKey: "k"),
            openRouter: FakeLlm(),
            domain: FakeAiDomain(),
            ocr: FakeAiOcr()
        )

        await vm.processImage()

        XCTAssertEqual(vm.toastMessage, strings.errorSelectImage)
    }

    func testProcessImage_emptyKey_showsSaveKeyToast() async {
        let strings = AIScannerStrings()
        let vm = AIScannerViewModel(
            strings: strings,
            keychain: FakeKeychain(apiKey: "   "),
            openRouter: FakeLlm(),
            domain: FakeAiDomain(),
            ocr: FakeAiOcr()
        )
        vm.selectedImage = UIImage()

        await vm.processImage()

        XCTAssertEqual(vm.toastMessage, strings.errorSaveKey)
    }

    func testProcessImage_duplicate_showsDuplicateError() async {
        let strings = AIScannerStrings()
        let domain = FakeAiDomain(duplicate: true)
        let vm = AIScannerViewModel(
            strings: strings,
            keychain: FakeKeychain(apiKey: "secret"),
            openRouter: FakeLlm(),
            domain: domain,
            ocr: FakeAiOcr(imageText: "hello")
        )
        vm.selectedImage = UIImage()

        await vm.processImage()

        if case let .error(msg) = vm.contentState {
            XCTAssertEqual(msg, strings.errorDuplicate)
        } else {
            XCTFail("Expected duplicate error, got \(vm.contentState)")
        }
        XCTAssertEqual(domain.saveCallCount, 0)
    }

    func testProcessImage_success_thenSave_callsDomainSave() async {
        let strings = AIScannerStrings()
        let parsed = AIMedicalData(
            documentType: "ОАК",
            institution: nil,
            doctorName: nil,
            analysisDate: nil,
            indicators: []
        )
        let llm = FakeLlm(result: parsed)
        let domain = FakeAiDomain(duplicate: false)
        let vm = AIScannerViewModel(
            strings: strings,
            keychain: FakeKeychain(apiKey: "secret"),
            openRouter: llm,
            domain: domain,
            ocr: FakeAiOcr(imageText: "ocr body")
        )
        vm.selectedImage = UIImage()

        await vm.processImage()

        guard case let .success(data, processedText: text) = vm.contentState else {
            return XCTFail("Expected success, got \(vm.contentState)")
        }
        XCTAssertEqual(text, "ocr body")
        XCTAssertEqual(data.documentType, parsed.documentType)
        XCTAssertEqual(domain.saveCallCount, 0)

        vm.saveDocument()

        XCTAssertEqual(domain.saveCallCount, 1)
        XCTAssertEqual(domain.lastProcessedText, "ocr body")
        XCTAssertEqual(vm.toastMessage, strings.toastAddedToDocuments)
        XCTAssertNil(vm.selectedImage)
    }

    func testProcessImage_invalidKeyFromLlm_showsSaveKeyError() async {
        let strings = AIScannerStrings()
        let llm = FakeLlm(throwing: OpenRouterService.AIError.invalidKey)
        let vm = AIScannerViewModel(
            strings: strings,
            keychain: FakeKeychain(apiKey: "secret"),
            openRouter: llm,
            domain: FakeAiDomain(),
            ocr: FakeAiOcr(imageText: "text")
        )
        vm.selectedImage = UIImage()

        await vm.processImage()

        if case let .error(msg) = vm.contentState {
            XCTAssertEqual(msg, strings.errorSaveKey)
        } else {
            XCTFail("Expected error, got \(vm.contentState)")
        }
    }
}

// MARK: - Fakes

private final class FakeKeychain: AIScannerKeychainReading {
    let apiKey: String
    init(apiKey: String) { self.apiKey = apiKey }
}

private final class FakeLlm: AIScannerLlmClient {
    var result: AIMedicalData?
    var error: Error?

    init(result: AIMedicalData? = nil, throwing: Error? = nil) {
        self.result = result
        self.error = throwing
    }

    func parseWithLlm(apiKey: String, cleanText: String) async throws -> AIMedicalData {
        if let error { throw error }
        return result ?? AIMedicalData(documentType: "X", institution: nil, doctorName: nil, analysisDate: nil, indicators: [])
    }
}

private final class FakeAiDomain: AIScannerDomainBackend {
    var duplicate: Bool
    var saveCallCount = 0
    var lastProcessedText: String?

    init(duplicate: Bool = false) {
        self.duplicate = duplicate
    }

    func checkDuplicate(cleanText: String) -> Bool { duplicate }

    func saveDocument(data: DomainMedicalData, processedText: String) {
        saveCallCount += 1
        lastProcessedText = processedText
    }
}

private final class FakeAiOcr: AIScannerOcrProviding {
    var imageText: String
    var pdfText: String?

    init(imageText: String = "", pdfText: String? = nil) {
        self.imageText = imageText
        self.pdfText = pdfText
    }

    func extractPdfText(from url: URL) async -> String? { pdfText }

    func extractText(from image: UIImage) throws -> String { imageText }
}

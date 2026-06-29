import XCTest
@testable import DocHint
import UIKit
import Shared

@MainActor
final class OfflineScannerViewModelTests: XCTestCase {

    func testProcessFile_withoutSelection_keepsIdle() async {
        let vm = OfflineScannerViewModel(
            textExtractor: FakeOfflineTextExtractor(),
            scanning: FakeOfflineScanning()
        )
        await vm.processFile()
        if case .idle = vm.contentState { } else {
            XCTFail("Expected idle, got \(vm.contentState)")
        }
    }

    func testProcessFile_docx_showsUnsupportedError() async {
        let strings = OfflineScannerStrings()
        let vm = OfflineScannerViewModel(
            textExtractor: FakeOfflineTextExtractor(),
            scanning: FakeOfflineScanning(),
            strings: strings
        )
        vm.selectedURL = URL(fileURLWithPath: "/tmp/x.docx")
        vm.fileType = .docx

        await vm.processFile()

        if case let .error(msg) = vm.contentState {
            XCTAssertEqual(msg, strings.errorDocxUnsupported)
        } else {
            XCTFail("Expected error(docx), got \(vm.contentState)")
        }
    }

    func testProcessFile_emptyExtract_showsExtractFailed() async {
        let strings = OfflineScannerStrings()
        let extract = FakeOfflineTextExtractor(returnText: nil)
        let vm = OfflineScannerViewModel(textExtractor: extract, scanning: FakeOfflineScanning(), strings: strings)
        vm.selectedURL = URL(fileURLWithPath: "/tmp/x.png")
        vm.fileType = .image

        await vm.processFile()

        if case let .error(msg) = vm.contentState {
            XCTAssertEqual(msg, strings.errorExtractFailed)
        } else {
            XCTFail("Expected extract error, got \(vm.contentState)")
        }
    }

    func testProcessFile_duplicate_showsDuplicateMessage() async {
        let strings = OfflineScannerStrings()
        let extract = FakeOfflineTextExtractor(returnText: "some ocr text")
        let scan = FakeOfflineScanning(result: OfflineScanProcessResult(data: nil, processedText: nil, error: .duplicateDocument))
        let vm = OfflineScannerViewModel(textExtractor: extract, scanning: scan, strings: strings)
        vm.selectedURL = URL(fileURLWithPath: "/tmp/x.png")
        vm.fileType = .image

        await vm.processFile()

        if case let .error(msg) = vm.contentState {
            XCTAssertEqual(msg, strings.errorDuplicate)
        } else {
            XCTFail("Expected duplicate error, got \(vm.contentState)")
        }
        XCTAssertEqual(scan.saveCallCount, 0)
    }

    func testProcessFile_success_thenSaveDocument_callsSaveAndResets() async {
        let strings = OfflineScannerStrings()
        let data = DomainHousingPaymentDocument(
            documentType: "Квитанция ЖКУ",
            institution: "ООО УК ФЛАГМАН",
            documentDate: "2026-02",
            source: nil,
            category: .main,
            documentNumber: nil,
            paymentDocumentId: nil,
            personalAccountNumber: "0667000001",
            unifiedPersonalAccount: nil,
            housingUtilitiesId: nil,
            propertyAddress: nil,
            payerName: nil,
            totalAreaSqm: nil,
            livingAreaSqm: nil,
            residentsCount: nil,
            amountDueForPeriod: "3625.82",
            amountPaid: nil,
            lastPaymentDate: nil,
            debtFromPreviousPeriods: nil,
            serviceLines: []
        )
        let extract = FakeOfflineTextExtractor(returnText: "raw")
        let scan = FakeOfflineScanning(
            result: OfflineScanProcessResult(data: data, processedText: "clean", error: nil)
        )
        let vm = OfflineScannerViewModel(textExtractor: extract, scanning: scan, strings: strings)
        vm.selectedURL = URL(fileURLWithPath: "/tmp/x.png")
        vm.fileType = .image

        await vm.processFile()

        guard case let .success(gotData, processedText: text) = vm.contentState else {
            return XCTFail("Expected success, got \(vm.contentState)")
        }
        XCTAssertEqual(text, "clean")
        XCTAssertEqual(gotData.documentType, data.documentType)
        XCTAssertEqual(scan.saveCallCount, 0)

        vm.saveDocument(data: gotData, processedText: text)

        XCTAssertEqual(scan.saveCallCount, 1)
        XCTAssertEqual(scan.lastSavedData?.documentType, data.documentType)
        XCTAssertEqual(scan.lastSavedText, "clean")
        XCTAssertEqual(vm.toastMessage, strings.toastAddedToDocuments)
        XCTAssertNil(vm.selectedURL)
        if case .idle = vm.contentState { } else {
            XCTFail("Expected idle after save, got \(vm.contentState)")
        }
    }
}

// MARK: - Fakes

private final class FakeOfflineTextExtractor: OfflineScannerTextExtracting {
    var returnText: String?

    init(returnText: String? = nil) {
        self.returnText = returnText
    }

    func extract(from url: URL, fileType: OfflineFileType) async -> String? {
        returnText
    }

    func loadPdfPreview(url: URL) async -> UIImage? { nil }
}

private final class FakeOfflineScanning: OfflineScanningBackend {
    var result: OfflineScanProcessResult
    var saveCallCount = 0
    var lastSavedData: DomainHousingPaymentDocument?
    var lastSavedText: String?

    init(result: OfflineScanProcessResult = OfflineScanProcessResult(data: nil, processedText: nil, error: .unknown)) {
        self.result = result
    }

    func processText(rawText: String) -> OfflineScanProcessResult {
        result
    }

    func saveDocument(data: DomainHousingPaymentDocument, processedText: String) {
        saveCallCount += 1
        lastSavedData = data
        lastSavedText = processedText
    }
}

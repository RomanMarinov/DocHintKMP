import Foundation

struct OfflineScannerStrings {
    var screenTitle: String { String(localized: "offline_screen_title") }
    var selectFile: String { String(localized: "offline_select_file") }
    var supportedFormats: String { String(localized: "offline_supported_formats") }
    var instructions: String { String(localized: "offline_instructions") }
    var recognize: String { String(localized: "offline_recognize") }
    var saveToDocuments: String { String(localized: "offline_save_to_documents") }
    var loadingOcr: String { String(localized: "offline_loading_ocr") }
    var loadingCleanup: String { String(localized: "offline_loading_cleanup") }
    var viewResult: String { String(localized: "offline_view_result") }
    var labelDocumentType: String { String(localized: "label_document_type") }
    var labelInstitution: String { String(localized: "label_institution") }
    var labelDoctor: String { String(localized: "label_doctor") }
    var labelAnalysisDate: String { String(localized: "label_analysis_date") }
    func indicatorsCount(_ n: Int) -> String {
        String(format: String(localized: "offline_indicators_count"), n)
    }

    var toastFileOpenFailed: String { String(localized: "toast_file_open_failed") }
    var toastAddedToDocuments: String { String(localized: "toast_added_to_documents") }
    var errorExtractFailed: String { String(localized: "error_extract_failed") }
    var errorDocxUnsupported: String { String(localized: "error_docx_unsupported") }
    var errorEmptyText: String { String(localized: "error_empty_text") }
    var errorTooShort: String { String(localized: "error_too_short") }
    var errorNoMedicalData: String { String(localized: "error_no_medical_data") }
    var errorDuplicate: String { String(localized: "error_duplicate") }
    var errorParseFailed: String { String(localized: "error_parse_failed") }
    var errorUnknown: String { String(localized: "error_unknown") }
}

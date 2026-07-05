import Foundation

struct OfflineScannerStrings {
    var screenTitle: String { String(localized: "offline_screen_title") }
    var selectFile: String { String(localized: "offline_select_file") }
    var supportedFormats: String { String(localized: "offline_supported_formats") }
    var instructions: String { String(localized: "offline_instructions") }
    var recognize: String { String(localized: "offline_recognize") }
    var processing: String { String(localized: "ai_processing") }
    var saveToDocuments: String { String(localized: "offline_save_to_documents") }
    var loadingOcr: String { String(localized: "offline_loading_ocr") }
    var loadingCleanup: String { String(localized: "offline_loading_cleanup") }
    var pipelineExtract: String { String(localized: "offline_pipeline_extract") }
    var viewResult: String { String(localized: "offline_view_result") }
    var labelDocumentType: String { String(localized: "label_document_type") }
    var labelProvider: String { String(localized: "label_provider") }
    var labelBillingPeriod: String { String(localized: "label_billing_period") }
    var labelPersonalAccount: String { String(localized: "label_personal_account") }
    var labelPropertyAddress: String { String(localized: "label_property_address") }
    var labelAmountDue: String { String(localized: "label_amount_due") }
    var labelAmountPaid: String { String(localized: "label_amount_paid", defaultValue: "Оплачено") }
    var labelPaymentDate: String { String(localized: "label_payment_date", defaultValue: "Дата оплаты") }
    var labelPayer: String { String(localized: "label_payer", defaultValue: "Плательщик") }
    var labelHousingUtilitiesId: String { String(localized: "label_housing_utilities_id", defaultValue: "ИЖКУ") }
    var labelPaymentDocumentId: String { String(localized: "label_payment_document_id", defaultValue: "ID документа") }
    var labelTotalAreaShort: String { String(localized: "label_total_area_short", defaultValue: "Общая") }
    var openPdf: String { String(localized: "button_open_pdf", defaultValue: "Open PDF") }
    var labelLivingAreaShort: String { String(localized: "label_living_area_short", defaultValue: "Жилая") }
    var labelResidents: String { String(localized: "label_residents", defaultValue: "Проживающих") }
    func servicesCount(_ n: Int) -> String {
        String(format: String(localized: "services_count"), n)
    }
    func tariffValue(_ value: String) -> String {
        String(format: String(localized: "tariff_value_format"), value)
    }

    var toastFileOpenFailed: String { String(localized: "toast_file_open_failed") }
    var toastAddedToDocuments: String { String(localized: "toast_added_to_documents") }
    var errorExtractFailed: String { String(localized: "error_extract_failed") }
    var errorDocxUnsupported: String { String(localized: "error_docx_unsupported") }
    var errorEmptyText: String { String(localized: "error_empty_text") }
    var errorTooShort: String { String(localized: "error_too_short") }
    var errorNoHousingBill: String { String(localized: "error_offline_not_housing_bill") }
    var errorDuplicate: String { String(localized: "error_duplicate") }
    var errorParseFailed: String { String(localized: "error_parse_failed") }
    var errorUnknown: String { String(localized: "error_unknown") }
}

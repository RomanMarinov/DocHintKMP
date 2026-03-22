import Foundation

/// Строки экрана Documents. Как stringResource в Android — язык подхватывается из системы.
struct DocumentsStrings {
    var screenDocuments: String { String(localized: "documents_screen_title") }
    var noSavedAnalyses: String { String(localized: "documents_no_saved") }
    var allClearData: String { String(localized: "documents_clear_all") }
    var emptyTitle: String { String(localized: "documents_empty_title") }
    var emptySubtitle: String { String(localized: "documents_empty_subtitle") }
    var analysisDefault: String { String(localized: "documents_analysis_default") }
    var labelDocumentType: String { String(localized: "label_document_type") }
    var labelInstitution: String { String(localized: "label_institution") }
    var labelDoctor: String { String(localized: "label_doctor") }
    var dialogDeleteTitle: String { String(localized: "documents_dialog_delete_title") }
    var dialogDeleteMessage: String { String(localized: "documents_dialog_delete_message") }
    var dialogClearTitle: String { String(localized: "documents_dialog_clear_title") }
    var dialogClearMessage: String { String(localized: "documents_dialog_clear_message") }
    var dialogConfirm: String { String(localized: "dialog_confirm") }
    var dialogCancel: String { String(localized: "dialog_cancel") }

    func indicatorsFormat(_ n: Int32) -> String {
        String(format: String(localized: "documents_indicators_format"), n)
    }

    func resultsCountText(_ n: Int) -> String {
        let format: String
        switch n {
        case 1: format = String(localized: "documents_analysis_one")
        case 2...4: format = String(localized: "documents_analysis_few")
        default: format = String(localized: "documents_analysis_many")
        }
        return String(format: format, n)
    }
}

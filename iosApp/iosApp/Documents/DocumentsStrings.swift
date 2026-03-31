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
    var dialogOk: String { String(localized: "dialog_ok", defaultValue: "OK") }
    var actionSend: String { String(localized: "documents_action_send") }
    var selectionCancel: String { String(localized: "documents_selection_cancel") }
    var share: String { String(localized: "documents_share") }
    var fabPickFirst: String { String(localized: "documents_fab_pick_first") }
    var tabMine: String { String(localized: "documents_tab_mine", defaultValue: "Документы") }
    var tabReceived: String { String(localized: "documents_tab_received", defaultValue: "Принятые") }
    var noImported: String { String(localized: "documents_no_imported", defaultValue: "Нет импортированных анализов") }
    var importCodeLabel: String { String(localized: "documents_import_code_label", defaultValue: "Одноразовый код") }
    var importButton: String { String(localized: "documents_import_button", defaultValue: "Импортировать по коду") }
    var importFailed: String { String(localized: "documents_import_failed", defaultValue: "Не удалось импортировать") }
    var shareFailed: String { String(localized: "documents_share_failed", defaultValue: "Не удалось отправить") }
    var dialogErrorTitle: String { String(localized: "dialog_error_title", defaultValue: "Ошибка") }
    var dialogImportResultTitle: String { String(localized: "dialog_import_result_title", defaultValue: "Результат импорта") }
    var selectAtLeastOne: String { String(localized: "documents_select_at_least_one", defaultValue: "Сначала выберите хотя бы один документ") }
    var shareCodeTitle: String { String(localized: "documents_share_code_title", defaultValue: "Код для передачи") }
    var copyCode: String { String(localized: "documents_copy_code", defaultValue: "Скопировать код") }

    func shareValidUntil(_ dateTime: String) -> String {
        String(format: String(localized: "documents_share_valid_until", defaultValue: "Действителен до: %@"), dateTime)
    }

    func importSummary(added: Int, skipped: Int) -> String {
        if added == 0 {
            return String(localized: "documents_import_summary_no_new", defaultValue: "Добавлено документов: 0\nНовых документов не найдено")
        }
        if skipped > 0 {
            return String(
                format: String(
                    localized: "documents_import_summary_added_and_skipped",
                    defaultValue: "Добавлено документов: %d\nОтклонено дубликатов: %d"
                ),
                added,
                skipped
            )
        }
        return String(
            format: String(localized: "documents_import_summary_added", defaultValue: "Добавлено документов: %d"),
            added
        )
    }

    func indicatorsFormat(_ n: Int32) -> String {
        String(format: String(localized: "documents_indicators_format"), n)
    }

    func selectedCount(_ n: Int) -> String {
        String(format: String(localized: "documents_selected_count"), n)
    }

    func resultsCountText(_ n: Int) -> String {
        if Locale.current.language.languageCode?.identifier == "ru" {
            return RussianQuantity.analysisCountLabel(count: n)
        }
        if n == 1 {
            return String(format: String(localized: "documents_analysis_one"), n)
        }
        return String(format: String(localized: "documents_analysis_many"), n)
    }
}

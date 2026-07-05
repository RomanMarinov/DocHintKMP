package app.romanmarinov.dochintkmp.presentation.documents_screen.model

sealed interface ResultsIntent {
    data class SelectTab(val tab: DocumentsTab) : ResultsIntent
    data class SetSelectionMode(val enabled: Boolean) : ResultsIntent
    data class ToggleSelect(val index: Int) : ResultsIntent
    data class DeleteRequest(val index: Int) : ResultsIntent
    data object DismissDeleteDialog : ResultsIntent
    data object ConfirmDelete : ResultsIntent
    data object ShowClearDialog : ResultsIntent
    data object DismissClearDialog : ResultsIntent
    data object ConfirmClear : ResultsIntent
    data class InputCodeChanged(val value: String) : ResultsIntent
    data object SendCode : ResultsIntent
    data object FabClicked : ResultsIntent
    data object DismissShareSheet : ResultsIntent
}

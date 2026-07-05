package app.romanmarinov.dochintkmp.presentation.documents_screen.model

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.DocumentsTab

data class ResultsState(
    val results: List<HousingPaymentDocument> = emptyList(),
    val selectedTab: DocumentsTab = DocumentsTab.MINE,
    val selectionMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val inputCode: String = "",
    val networkBusy: Boolean = false,
    val deleteIndex: Int? = null,
    val showClearDialog: Boolean = false,
    val shareSheet: ShareSheetState? = null
)

data class ShareSheetState(
    val code: String,
    val expiresAt: String
)

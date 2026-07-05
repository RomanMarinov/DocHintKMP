package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument

sealed interface OcrOfflineContentState {
    data object Idle : OcrOfflineContentState
    data object Loading : OcrOfflineContentState
    data class Success(val data: HousingPaymentDocument, val processedText: String) : OcrOfflineContentState
    data class Error(val type: OcrOfflineErrorType) : OcrOfflineContentState
}

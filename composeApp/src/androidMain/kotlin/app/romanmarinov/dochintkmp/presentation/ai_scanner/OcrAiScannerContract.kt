package app.romanmarinov.dochintkmp.presentation.ai_scanner

import android.net.Uri
import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.domain.model.ParseResult

data class OcrAiScannerState(
    val contentState: OcrAiContentState = OcrAiContentState.Idle,
    val hasSavedKey: Boolean = false,
    val selectedUri: Uri? = null
)

sealed interface OcrAiContentState {
    data object Idle : OcrAiContentState
    data object Loading : OcrAiContentState
    data class Success(val data: ParseResult) : OcrAiContentState
    data class Error(val type: OcrAiErrorType) : OcrAiContentState
}

enum class OcrAiErrorType(@StringRes val stringResId: Int) {
    SELECT_IMAGE(R.string.error_select_image),
    SAVE_API_KEY(R.string.error_save_api_key),
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_ai),
    NO_NETWORK(R.string.error_no_network),
    UNKNOWN(R.string.error_unknown),
}

enum class OcrAiToastType(@StringRes val stringResId: Int) {
    SELECT_IMAGE(R.string.error_select_image),
    SAVE_API_KEY(R.string.error_save_api_key),
    ADDED_TO_DOCUMENTS(R.string.toast_added_to_documents),
}

sealed interface OcrAiScannerEvent {
    data class SelectImage(val uri: Uri) : OcrAiScannerEvent
    data object ProcessImage : OcrAiScannerEvent
    data object ResetState : OcrAiScannerEvent
    data object RefreshKey : OcrAiScannerEvent
}

sealed interface OcrAiScannerEffect {
    data class ShowToast(val type: OcrAiToastType) : OcrAiScannerEffect
}

package app.romanmarinov.dochintkmp.presentation.offline_scanner

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.MedicalData

data class OcrOfflineScannerState(
    val contentState: OcrOfflineContentState = OcrOfflineContentState.Idle,
    val selectedUri: Uri? = null,
    val fileType: FileType = FileType.PDF,
    val pdfPreviewBitmap: Bitmap? = null
)

sealed interface OcrOfflineContentState {
    data object Idle : OcrOfflineContentState
    data object Loading : OcrOfflineContentState
    data class Success(val data: MedicalData) : OcrOfflineContentState
    data class Error(val type: OcrOfflineErrorType) : OcrOfflineContentState
}

enum class OcrOfflineErrorType(@StringRes val stringResId: Int) {
    SELECT_FILE(R.string.error_select_file),
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_offline),
    UNKNOWN(R.string.error_unknown),
}

enum class OcrOfflineToastType(@StringRes val stringResId: Int) {
    SELECT_FILE(R.string.error_select_file),
    ADDED_TO_DOCUMENTS(R.string.toast_added_to_documents),
}

sealed interface OcrOfflineScannerEvent {
    data class SelectFile(val uri: Uri, val fileType: FileType) : OcrOfflineScannerEvent
    data object ProcessFile : OcrOfflineScannerEvent
    data object SaveDocument : OcrOfflineScannerEvent
    data object ResetState : OcrOfflineScannerEvent
}

sealed interface OcrOfflineScannerEffect {
    data class ShowToast(val type: OcrOfflineToastType) : OcrOfflineScannerEffect
}

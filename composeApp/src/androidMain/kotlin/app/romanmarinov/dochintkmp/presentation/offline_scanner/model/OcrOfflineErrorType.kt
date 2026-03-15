package app.romanmarinov.dochintkmp.presentation.offline_scanner.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrOfflineErrorType(@StringRes val stringResId: Int) {
    SELECT_FILE(R.string.error_select_file),
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_offline),
    UNKNOWN(R.string.error_unknown),
}

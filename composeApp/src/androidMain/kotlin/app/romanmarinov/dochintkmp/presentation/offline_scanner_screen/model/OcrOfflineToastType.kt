package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrOfflineToastType(@StringRes val stringResId: Int) {
    SELECT_FILE(R.string.error_select_file),
    ADDED_TO_DOCUMENTS(R.string.toast_added_to_documents),
}

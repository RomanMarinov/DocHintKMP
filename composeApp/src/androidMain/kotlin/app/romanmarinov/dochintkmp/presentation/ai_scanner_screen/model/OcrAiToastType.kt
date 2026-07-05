package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrAiToastType(@StringRes val stringResId: Int) {
    SELECT_IMAGE(R.string.error_select_image),
    SAVE_API_KEY(R.string.error_save_api_key),
    ADDED_TO_DOCUMENTS(R.string.toast_added_to_documents),
}

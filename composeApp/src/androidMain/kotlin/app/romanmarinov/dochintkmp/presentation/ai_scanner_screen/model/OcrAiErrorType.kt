package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrAiErrorType(@param:StringRes val stringResId: Int) {
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_ai),
    NO_NETWORK(R.string.error_no_network),
    UNKNOWN(R.string.error_unknown),
}

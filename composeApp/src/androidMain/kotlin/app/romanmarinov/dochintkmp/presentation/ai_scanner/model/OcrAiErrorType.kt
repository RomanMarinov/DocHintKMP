package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrAiErrorType(@StringRes val stringResId: Int) {
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_ai),
    NO_NETWORK(R.string.error_no_network),
    UNKNOWN(R.string.error_unknown),
}

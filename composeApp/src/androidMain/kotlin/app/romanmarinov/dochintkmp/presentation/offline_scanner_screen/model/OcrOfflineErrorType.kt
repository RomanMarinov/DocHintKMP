package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

import androidx.annotation.StringRes
import app.romanmarinov.dochintkmp.R

enum class OcrOfflineErrorType(@StringRes val stringResId: Int) {
    SELECT_FILE(R.string.error_select_file),
    DUPLICATE_DOCUMENT(R.string.error_duplicate_document_offline),
    NOT_HOUSING_BILL(R.string.error_offline_not_housing_bill),
    TOO_SHORT(R.string.error_offline_too_short),
    NO_SERVICE_LINES(R.string.error_offline_no_service_lines),
    PARSE_FAILED(R.string.error_offline_parse_failed),
    UNKNOWN(R.string.error_unknown),
}

package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model

import app.romanmarinov.dochintkmp.domain.model.ParseResult

sealed interface OcrAiContentState {
    data object Idle : OcrAiContentState
    data object Loading : OcrAiContentState
    data class Success(val parseResult: ParseResult, val cleanText: String) : OcrAiContentState
    data class Error(val type: OcrAiErrorType, val detailMessage: String? = null) : OcrAiContentState
}

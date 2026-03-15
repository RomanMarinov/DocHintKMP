package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

import app.romanmarinov.dochintkmp.domain.model.ParseResult

sealed interface OcrAiContentState {
    data object Idle : OcrAiContentState
    data object Loading : OcrAiContentState
    data class Success(val data: ParseResult) : OcrAiContentState
    data class Error(val type: OcrAiErrorType) : OcrAiContentState
}

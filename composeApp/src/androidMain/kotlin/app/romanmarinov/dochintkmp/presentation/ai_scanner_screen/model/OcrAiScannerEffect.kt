package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model

sealed interface OcrAiScannerEffect {
    data class ShowToast(val type: OcrAiToastType) : OcrAiScannerEffect
}

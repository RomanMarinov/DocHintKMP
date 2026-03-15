package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

sealed interface OcrAiScannerEffect {
    data class ShowToast(val type: OcrAiToastType) : OcrAiScannerEffect
}

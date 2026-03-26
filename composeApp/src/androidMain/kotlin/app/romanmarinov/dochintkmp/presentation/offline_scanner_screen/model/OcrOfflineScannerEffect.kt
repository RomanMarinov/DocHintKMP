package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

sealed interface OcrOfflineScannerEffect {
    data class ShowToast(val type: OcrOfflineToastType) : OcrOfflineScannerEffect
}

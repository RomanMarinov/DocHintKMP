package app.romanmarinov.dochintkmp.presentation.offline_scanner.model

sealed interface OcrOfflineScannerEffect {
    data class ShowToast(val type: OcrOfflineToastType) : OcrOfflineScannerEffect
}

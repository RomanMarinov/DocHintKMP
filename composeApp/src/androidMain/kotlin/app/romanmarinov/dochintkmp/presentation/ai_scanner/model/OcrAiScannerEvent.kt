package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

import android.net.Uri

sealed interface OcrAiScannerEvent {
    data class SelectImage(val uri: Uri) : OcrAiScannerEvent
    data object ProcessImage : OcrAiScannerEvent
    data object ResetState : OcrAiScannerEvent
    data object RefreshKey : OcrAiScannerEvent
}

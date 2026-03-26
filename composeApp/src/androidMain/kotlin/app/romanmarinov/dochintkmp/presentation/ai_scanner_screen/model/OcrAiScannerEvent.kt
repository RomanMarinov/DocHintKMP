package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model

import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType

sealed interface OcrAiScannerEvent {
    data class SelectFile(val uri: Uri, val fileType: FileType) : OcrAiScannerEvent
    data object ProcessImage : OcrAiScannerEvent
    data object SaveDocument : OcrAiScannerEvent
    data object ResetState : OcrAiScannerEvent
    data object RefreshKey : OcrAiScannerEvent
}

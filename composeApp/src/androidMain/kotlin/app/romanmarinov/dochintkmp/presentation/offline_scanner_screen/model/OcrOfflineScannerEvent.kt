package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType

sealed interface OcrOfflineScannerEvent {
    data class SelectFile(val uri: Uri, val fileType: FileType) : OcrOfflineScannerEvent
    data object ProcessFile : OcrOfflineScannerEvent
    data object SaveDocument : OcrOfflineScannerEvent
    data object ResetState : OcrOfflineScannerEvent
}

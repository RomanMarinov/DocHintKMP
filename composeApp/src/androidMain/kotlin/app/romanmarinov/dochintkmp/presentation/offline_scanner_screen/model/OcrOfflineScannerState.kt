package app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model

import android.graphics.Bitmap
import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType

data class OcrOfflineScannerState(
    val contentState: OcrOfflineContentState = OcrOfflineContentState.Idle,
    val selectedUri: Uri? = null,
    val fileType: FileType = FileType.PDF,
    val pdfPreviewBitmap: Bitmap? = null,
    val toastType: OcrOfflineToastType? = null
)

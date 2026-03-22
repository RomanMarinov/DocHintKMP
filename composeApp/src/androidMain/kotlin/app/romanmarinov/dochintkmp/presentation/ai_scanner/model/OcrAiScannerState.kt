package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

import android.graphics.Bitmap
import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType

data class OcrAiScannerState(
    val contentState: OcrAiContentState = OcrAiContentState.Idle,
    val hasSavedKey: Boolean = false,
    val selectedUri: Uri? = null,
    val selectedFileType: FileType = FileType.IMAGE,
    val pdfPreviewBitmap: Bitmap? = null,
    val toastType: OcrAiToastType? = null
)

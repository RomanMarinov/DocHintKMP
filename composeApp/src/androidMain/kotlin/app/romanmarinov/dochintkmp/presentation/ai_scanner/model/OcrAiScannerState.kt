package app.romanmarinov.dochintkmp.presentation.ai_scanner.model

import android.net.Uri

data class OcrAiScannerState(
    val contentState: OcrAiContentState = OcrAiContentState.Idle,
    val hasSavedKey: Boolean = false,
    val selectedUri: Uri? = null
)

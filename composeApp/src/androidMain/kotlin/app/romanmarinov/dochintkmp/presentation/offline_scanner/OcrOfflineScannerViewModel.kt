package app.romanmarinov.dochintkmp.presentation.offline_scanner

import app.romanmarinov.dochintkmp.presentation.offline_scanner.model.OcrOfflineContentState
import app.romanmarinov.dochintkmp.presentation.offline_scanner.model.OcrOfflineErrorType
import app.romanmarinov.dochintkmp.presentation.offline_scanner.model.OcrOfflineScannerEvent
import app.romanmarinov.dochintkmp.presentation.offline_scanner.model.OcrOfflineScannerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
import app.romanmarinov.dochintkmp.data.parser.RuleParser
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri
import android.util.Log
import app.romanmarinov.dochintkmp.presentation.offline_scanner.model.OcrOfflineToastType

class OcrOfflineScannerViewModel(
    private val extractTextOffline: ExtractTextOfflineUseCase,
    private val checkDuplicate: CheckDuplicateUseCase,
    private val addResult: AddResultUseCase,
    private val ruleParser: RuleParser,
    private val pdfPageRenderer: PdfPageRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrOfflineScannerState())
    val uiState: StateFlow<OcrOfflineScannerState> = _uiState.asStateFlow()

    fun onEvent(event: OcrOfflineScannerEvent) {
        when (event) {
            is OcrOfflineScannerEvent.SelectFile -> selectFile(event.uri, event.fileType)
            is OcrOfflineScannerEvent.ProcessFile -> processFile()
            is OcrOfflineScannerEvent.SaveDocument -> saveDocument()
            is OcrOfflineScannerEvent.ResetState -> resetState()
        }
    }

    fun onToastShown() {
        _uiState.update { it.copy(toastType = null) }
    }

    private fun selectFile(uri: Uri, fileType: FileType) {
        _uiState.value.pdfPreviewBitmap?.recycle()
        _uiState.update {
            it.copy(
                selectedUri = uri,
                fileType = fileType,
                contentState = OcrOfflineContentState.Idle,
                pdfPreviewBitmap = null
            )
        }
        if (fileType == FileType.PDF) {
            viewModelScope.launch {
                val bitmap = pdfPageRenderer.renderFirstPageToBitmap(uri)
                _uiState.update { it.copy(pdfPreviewBitmap = bitmap) }
            }
        }
    }

    private fun resetState() {
        _uiState.value.pdfPreviewBitmap?.recycle()
        _uiState.update {
            it.copy(
                contentState = OcrOfflineContentState.Idle,
                selectedUri = null,
                pdfPreviewBitmap = null
            )
        }
    }

    private fun processFile() {
        val state = _uiState.value
        val uri = state.selectedUri ?: run {
            _uiState.update { it.copy(toastType = OcrOfflineToastType.SELECT_FILE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(contentState = OcrOfflineContentState.Loading) }
            try {
                val cleanText = extractTextOffline(uri, state.fileType)
                if (checkDuplicate(cleanText)) {
                    _uiState.update { it.copy(contentState = OcrOfflineContentState.Error(OcrOfflineErrorType.DUPLICATE_DOCUMENT)) }
                    return@launch
                }
                val data = ruleParser.parse(cleanText)
                _uiState.update { it.copy(contentState = OcrOfflineContentState.Success(data, cleanText)) }
            } catch (e: Exception) {
                Log.e("OcrOfflineScanner", "processFile", e)
                _uiState.update { it.copy(contentState = OcrOfflineContentState.Error(OcrOfflineErrorType.UNKNOWN)) }
            }
        }
    }

    private fun saveDocument() {
        val success = _uiState.value.contentState as? OcrOfflineContentState.Success ?: return
        viewModelScope.launch {
            addResult(success.data, success.processedText)
            _uiState.update { it.copy(toastType = OcrOfflineToastType.ADDED_TO_DOCUMENTS) }
            resetState()
        }
    }
}

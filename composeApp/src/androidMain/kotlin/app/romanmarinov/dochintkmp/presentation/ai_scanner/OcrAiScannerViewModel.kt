package app.romanmarinov.dochintkmp.presentation.ai_scanner

import app.romanmarinov.dochintkmp.presentation.ai_scanner.model.OcrAiContentState
import app.romanmarinov.dochintkmp.presentation.ai_scanner.model.OcrAiErrorType
import app.romanmarinov.dochintkmp.presentation.ai_scanner.model.OcrAiScannerEvent
import app.romanmarinov.dochintkmp.presentation.ai_scanner.model.OcrAiScannerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import app.romanmarinov.dochintkmp.data.local.SecureStorage
import app.romanmarinov.dochintkmp.data.ocr.PdfPageRenderer
import app.romanmarinov.dochintkmp.data.usecase.ExtractTextOfflineUseCase
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ExtractTextFromImageUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ParseWithLlmUseCase
import app.romanmarinov.dochintkmp.presentation.ai_scanner.model.OcrAiToastType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class OcrAiScannerViewModel(
    private val extractTextFromImage: ExtractTextFromImageUseCase,
    private val extractTextOffline: ExtractTextOfflineUseCase,
    private val parseWithLlm: ParseWithLlmUseCase,
    private val checkDuplicate: CheckDuplicateUseCase,
    private val addResult: AddResultUseCase,
    private val secureStorage: SecureStorage,
    private val pdfPageRenderer: PdfPageRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrAiScannerState(hasSavedKey = secureStorage.apiKey.isNotEmpty()))
    val uiState: StateFlow<OcrAiScannerState> = _uiState.asStateFlow()

    fun onEvent(event: OcrAiScannerEvent) {
        when (event) {
            is OcrAiScannerEvent.SelectFile -> selectFile(event.uri, event.fileType)
            is OcrAiScannerEvent.ProcessImage -> processImage()
            is OcrAiScannerEvent.SaveDocument -> saveDocument()
            is OcrAiScannerEvent.ResetState -> resetState()
            is OcrAiScannerEvent.RefreshKey -> updateState { it.copy(hasSavedKey = secureStorage.apiKey.isNotEmpty()) }
        }
    }

    fun onToastShown() {
        updateState { it.copy(toastType = null) }
    }

    private fun selectFile(uri: Uri, fileType: FileType) {
        _uiState.value.pdfPreviewBitmap?.recycle()
        updateState {
            it.copy(
                selectedUri = uri,
                selectedFileType = fileType,
                contentState = OcrAiContentState.Idle,
                pdfPreviewBitmap = null
            )
        }
        if (fileType == FileType.PDF) {
            viewModelScope.launch {
                val bitmap = pdfPageRenderer.renderFirstPageToBitmap(uri)
                updateState { it.copy(pdfPreviewBitmap = bitmap) }
            }
        }
    }

    private fun resetState() {
        _uiState.value.pdfPreviewBitmap?.recycle()
        updateState {
            it.copy(
                contentState = OcrAiContentState.Idle,
                selectedUri = null,
                selectedFileType = FileType.IMAGE,
                pdfPreviewBitmap = null
            )
        }
    }

    private fun processImage() {
        val uri = _uiState.value.selectedUri ?: run {
            updateState { it.copy(toastType = OcrAiToastType.SELECT_IMAGE) }
            return
        }
        val key = secureStorage.apiKey.trim()
        if (key.isEmpty()) {
            updateState { it.copy(toastType = OcrAiToastType.SAVE_API_KEY) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(contentState = OcrAiContentState.Loading) }
            try {
                val cleanText = when (_uiState.value.selectedFileType) {
                    FileType.PDF, FileType.TXT, FileType.DOCX ->
                        extractTextOffline(uri, _uiState.value.selectedFileType)
                    FileType.IMAGE, FileType.PNG ->
                        extractTextFromImage(uri.toString())
                }

                if (checkDuplicate(cleanText)) {
                    updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.DUPLICATE_DOCUMENT)) }
                    return@launch
                }

                val result = parseWithLlm(key, cleanText)

                updateState { it.copy(contentState = OcrAiContentState.Success(result, cleanText)) }
            } catch (e: UnknownHostException) {
                updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.NO_NETWORK)) }
            } catch (e: Exception) {
                val msg = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
                updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.UNKNOWN, msg)) }
            }
        }
    }

    private fun saveDocument() {
        val success = _uiState.value.contentState as? OcrAiContentState.Success ?: return
        viewModelScope.launch {
            addResult(success.parseResult.data, success.cleanText, ResultsRepository.SOURCE_AI)
            updateState { it.copy(toastType = OcrAiToastType.ADDED_TO_DOCUMENTS) }
            resetState()
        }
    }

    private fun updateState(block: (OcrAiScannerState) -> OcrAiScannerState) {
        _uiState.update(block)
    }
}

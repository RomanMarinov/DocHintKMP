package app.romanmarinov.dochintkmp.presentation.ai_scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.romanmarinov.dochintkmp.data.local.SecureStorage
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.CheckDuplicateUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ExtractTextFromImageUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ParseWithLlmUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class OcrAiScannerViewModel(
    private val extractTextFromImage: ExtractTextFromImageUseCase,
    private val parseWithLlm: ParseWithLlmUseCase,
    private val checkDuplicate: CheckDuplicateUseCase,
    private val addResult: AddResultUseCase,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrAiScannerState(hasSavedKey = secureStorage.apiKey.isNotEmpty()))
    val uiState: StateFlow<OcrAiScannerState> = _uiState.asStateFlow()

    private val _effect = Channel<OcrAiScannerEffect>()
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: OcrAiScannerEvent) {
        when (event) {
            is OcrAiScannerEvent.SelectImage -> updateState { it.copy(selectedUri = event.uri, contentState = OcrAiContentState.Idle) }
            is OcrAiScannerEvent.ProcessImage -> processImage()
            is OcrAiScannerEvent.ResetState -> updateState { it.copy(contentState = OcrAiContentState.Idle, selectedUri = null) }
            is OcrAiScannerEvent.RefreshKey -> updateState { it.copy(hasSavedKey = secureStorage.apiKey.isNotEmpty()) }
        }
    }

    private fun processImage() {
        val uri = _uiState.value.selectedUri ?: run {
            viewModelScope.launch { _effect.send(OcrAiScannerEffect.ShowToast(OcrAiToastType.SELECT_IMAGE)) }
            return
        }
        val key = secureStorage.apiKey.trim()
        if (key.isEmpty()) {
            viewModelScope.launch { _effect.send(OcrAiScannerEffect.ShowToast(OcrAiToastType.SAVE_API_KEY)) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(contentState = OcrAiContentState.Loading) }
            try {
                val cleanText = extractTextFromImage(uri.toString())

                if (checkDuplicate(cleanText)) {
                    updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.DUPLICATE_DOCUMENT)) }
                    return@launch
                }

                val result = parseWithLlm(key, cleanText)

                updateState { it.copy(contentState = OcrAiContentState.Success(result)) }
                addResult(result.data)
                _effect.send(OcrAiScannerEffect.ShowToast(OcrAiToastType.ADDED_TO_DOCUMENTS))
            } catch (e: UnknownHostException) {
                updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.NO_NETWORK)) }
            } catch (e: Exception) {
                updateState { it.copy(contentState = OcrAiContentState.Error(OcrAiErrorType.UNKNOWN)) }
            }
        }
    }

    private fun updateState(block: (OcrAiScannerState) -> OcrAiScannerState) {
        _uiState.update(block)
    }
}

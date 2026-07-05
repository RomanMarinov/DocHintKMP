package app.romanmarinov.androidapp

import android.net.Uri
import app.romanmarinov.dochintkmp.data.local.ApiKeyStorage
import app.romanmarinov.dochintkmp.data.ocr.PdfFirstPageRenderer
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.AiScannerPort
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.OcrAiScannerViewModel
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiContentState
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiErrorType
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiScannerEvent
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiToastType
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@RunWith(RobolectricTestRunner::class)
class OcrAiScannerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private fun testUri(): Uri = Uri.parse("content://test")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun processImage_whenNoSelectedUri_setsSelectImageToast() = runTest {
        val port = FakePort(cleanText = "clean")
        val apiKey = FakeApiKeyStorage(apiKey = "key")
        val pdf = FakePdfRenderer()

        val vm = OcrAiScannerViewModel(port, apiKey, pdf)

        vm.onEvent(OcrAiScannerEvent.ProcessImage)

        assertEquals(OcrAiToastType.SELECT_IMAGE, vm.uiState.value.toastType)
    }

    @Test
    fun processImage_whenApiKeyEmpty_setsSaveApiKeyToast_andDoesNotSave() = runTest {
        val port = FakePort(cleanText = "clean")
        val apiKey = FakeApiKeyStorage(apiKey = "")
        val pdf = FakePdfRenderer()

        val vm = OcrAiScannerViewModel(port, apiKey, pdf)
        vm.onEvent(OcrAiScannerEvent.SelectFile(testUri(), FileType.IMAGE))

        vm.onEvent(OcrAiScannerEvent.ProcessImage)

        assertEquals(OcrAiToastType.SAVE_API_KEY, vm.uiState.value.toastType)
        assertTrue(port.addCallsCount == 0)
        assertTrue(vm.uiState.value.contentState is OcrAiContentState.Idle)
    }

    @Test
    fun processImage_whenDuplicate_setsDuplicateError_andDoesNotSave() = runTest {
        val port = FakePort(cleanText = "clean", duplicate = true)
        val apiKey = FakeApiKeyStorage(apiKey = "key")
        val pdf = FakePdfRenderer()

        val vm = OcrAiScannerViewModel(port, apiKey, pdf)
        vm.onEvent(OcrAiScannerEvent.SelectFile(testUri(), FileType.IMAGE))

        vm.onEvent(OcrAiScannerEvent.ProcessImage)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.contentState is OcrAiContentState.Error)
        val error = state.contentState as OcrAiContentState.Error
        assertEquals(OcrAiErrorType.DUPLICATE_DOCUMENT, error.type)
        assertNull(state.toastType)
        assertTrue(port.addCallsCount == 0)
    }

    @Test
    fun processImage_whenSuccess_savesOnlyAfterSaveDocument() = runTest {
        val cleanText = "clean-text"
        val data = HousingPaymentDocument(documentType = "Квитанция ЖКУ", institution = "ООО УК")
        val parseResult = ParseResult(housingDocument = data, promptTokens = 1, completionTokens = 2, totalTokens = 3)

        val port = FakePort(cleanText = cleanText, duplicate = false, parseResult = parseResult)
        val apiKey = FakeApiKeyStorage(apiKey = "key")
        val pdf = FakePdfRenderer()

        val vm = OcrAiScannerViewModel(port, apiKey, pdf)
        vm.onEvent(OcrAiScannerEvent.SelectFile(testUri(), FileType.IMAGE))

        vm.onEvent(OcrAiScannerEvent.ProcessImage)
        advanceUntilIdle()

        var state = vm.uiState.value
        assertTrue(state.contentState is OcrAiContentState.Success)
        val success = state.contentState as OcrAiContentState.Success
        assertEquals(parseResult, success.parseResult)
        assertEquals(cleanText, success.cleanText)
        assertNull(state.toastType)
        assertTrue(port.addCallsCount == 0)

        vm.onEvent(OcrAiScannerEvent.SaveDocument)
        advanceUntilIdle()

        state = vm.uiState.value
        assertEquals(1, port.addCallsCount)
        assertEquals(data, port.lastAddedData)
        assertEquals(cleanText, port.lastAddedProcessedText)
        assertEquals(OcrAiToastType.ADDED_TO_DOCUMENTS, state.toastType)
        assertTrue(state.contentState is OcrAiContentState.Idle)
        assertNull(state.selectedUri)
    }

    @Test
    fun processImage_whenParseThrowsUnknownHost_setsNoNetworkError() = runTest {
        val port = FakePort(
            cleanText = "clean",
            parseThrows = java.net.UnknownHostException("no net")
        )
        val apiKey = FakeApiKeyStorage(apiKey = "key")
        val pdf = FakePdfRenderer()

        val vm = OcrAiScannerViewModel(port, apiKey, pdf)
        vm.onEvent(OcrAiScannerEvent.SelectFile(testUri(), FileType.IMAGE))

        vm.onEvent(OcrAiScannerEvent.ProcessImage)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.contentState is OcrAiContentState.Error)
        val error = state.contentState as OcrAiContentState.Error
        assertEquals(OcrAiErrorType.NO_NETWORK, error.type)
    }

    private class FakeApiKeyStorage(override var apiKey: String) : ApiKeyStorage

    private class FakePdfRenderer : PdfFirstPageRenderer {
        override suspend fun renderFirstPageToBitmap(uri: Uri) = null
    }

    private class FakePort(
        private val cleanText: String,
        private val duplicate: Boolean = false,
        private val parseResult: ParseResult = ParseResult(housingDocument = HousingPaymentDocument()),
        private val parseThrows: Throwable? = null
    ) : AiScannerPort {
        var addCallsCount: Int = 0
        var lastAddedData: HousingPaymentDocument? = null
        var lastAddedProcessedText: String? = null

        override suspend fun extractTextOffline(uri: Uri, fileType: FileType): String = cleanText

        override suspend fun extractTextFromImage(fileRef: String): String = cleanText

        override suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult {
            parseThrows?.let { throw it }
            return parseResult
        }

        override fun isDuplicate(cleanText: String): Boolean = duplicate

        override fun addResult(data: HousingPaymentDocument, processedText: String) {
            addCallsCount += 1
            lastAddedData = data
            lastAddedProcessedText = processedText
        }
    }
}


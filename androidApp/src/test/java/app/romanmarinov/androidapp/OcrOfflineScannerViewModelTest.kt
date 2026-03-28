package app.romanmarinov.androidapp

import android.net.Uri
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerPort
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.OcrOfflineScannerViewModel
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model.OcrOfflineContentState
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model.OcrOfflineErrorType
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model.OcrOfflineScannerEvent
import app.romanmarinov.dochintkmp.presentation.offline_scanner_screen.model.OcrOfflineToastType
import app.romanmarinov.dochintkmp.data.ocr.PdfFirstPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class OcrOfflineScannerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun processFile_whenNoSelectedUri_setsSelectFileToast() = runTest {
        val port = FakePort(cleanText = "clean")
        val pdf = FakePdfRenderer()
        val vm = OcrOfflineScannerViewModel(port, pdf)

        vm.onEvent(OcrOfflineScannerEvent.ProcessFile)
        advanceUntilIdle()

        assertEquals(OcrOfflineToastType.SELECT_FILE, vm.uiState.value.toastType)
        assertTrue(vm.uiState.value.selectedUri == null)
        assertTrue(vm.uiState.value.contentState is OcrOfflineContentState.Idle)
    }

    @Test
    fun processFile_whenDuplicate_setsDuplicateError_andDoesNotSave() = runTest {
        val port = FakePort(cleanText = "clean", duplicate = true)
        val pdf = FakePdfRenderer()
        val vm = OcrOfflineScannerViewModel(port, pdf)

        vm.onEvent(OcrOfflineScannerEvent.SelectFile(Uri.parse("content://test"), FileType.IMAGE))
        vm.onEvent(OcrOfflineScannerEvent.ProcessFile)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.contentState is OcrOfflineContentState.Error)
        val error = state.contentState as OcrOfflineContentState.Error
        assertEquals(OcrOfflineErrorType.DUPLICATE_DOCUMENT, error.type)
        assertNull(state.toastType)
        assertTrue(port.addCallsCount == 0)
    }

    @Test
    fun processFile_whenSuccess_savesOnlyAfterSaveDocument() = runTest {
        val cleanText = "clean-text"
        val data = MedicalData(documentType = "ОАК", institution = "Invitro")

        val port = FakePort(cleanText = cleanText, duplicate = false, parseData = data)
        val pdf = FakePdfRenderer()
        val vm = OcrOfflineScannerViewModel(port, pdf)

        vm.onEvent(OcrOfflineScannerEvent.SelectFile(Uri.parse("content://test"), FileType.IMAGE))
        vm.onEvent(OcrOfflineScannerEvent.ProcessFile)
        advanceUntilIdle()

        var state = vm.uiState.value
        assertTrue(state.contentState is OcrOfflineContentState.Success)
        val success = state.contentState as OcrOfflineContentState.Success
        assertEquals(data, success.data)
        assertEquals(cleanText, success.processedText)
        assertNull(state.toastType)
        assertTrue(port.addCallsCount == 0)

        vm.onEvent(OcrOfflineScannerEvent.SaveDocument)
        advanceUntilIdle()

        state = vm.uiState.value
        assertEquals(1, port.addCallsCount)
        assertEquals(data, port.lastAddedData)
        assertEquals(cleanText, port.lastAddedProcessedText)
        assertEquals(OcrOfflineToastType.ADDED_TO_DOCUMENTS, state.toastType)
        assertTrue(state.contentState is OcrOfflineContentState.Idle)
        assertNull(state.selectedUri)
    }

    @Test
    fun processFile_whenExtractThrows_setsUnknownError() = runTest {
        val port = FakePort(cleanText = "clean", extractThrows = RuntimeException("boom"))
        val pdf = FakePdfRenderer()
        val vm = OcrOfflineScannerViewModel(port, pdf)

        vm.onEvent(OcrOfflineScannerEvent.SelectFile(Uri.parse("content://test"), FileType.IMAGE))
        vm.onEvent(OcrOfflineScannerEvent.ProcessFile)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.contentState is OcrOfflineContentState.Error)
        val error = state.contentState as OcrOfflineContentState.Error
        assertEquals(OcrOfflineErrorType.UNKNOWN, error.type)
        assertNull(state.toastType)
        assertTrue(port.addCallsCount == 0)
    }

    private class FakePdfRenderer : PdfFirstPageRenderer {
        override suspend fun renderFirstPageToBitmap(uri: Uri) = null
    }

    private class FakePort(
        private val cleanText: String,
        private val duplicate: Boolean = false,
        private val parseData: MedicalData = MedicalData(),
        private val extractThrows: Throwable? = null,
    ) : OcrOfflineScannerPort {
        var addCallsCount: Int = 0
        var lastAddedData: MedicalData? = null
        var lastAddedProcessedText: String? = null

        override suspend fun extractTextOffline(uri: Uri, fileType: FileType): String {
            extractThrows?.let { throw it }
            return cleanText
        }

        override fun isDuplicate(cleanText: String): Boolean = duplicate

        override fun parse(cleanText: String): MedicalData = parseData

        override fun addResult(data: MedicalData, processedText: String) {
            addCallsCount += 1
            lastAddedData = data
            lastAddedProcessedText = processedText
        }
    }
}


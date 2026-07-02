package app.romanmarinov.dochintkmp.domain.usecase

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.domain.repository.DocumentParseRepository
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UseCasesTest {

    @Test
    fun addResultUseCase_addsResult_andMarksProcessed_whenProcessedTextProvided() {
        val repo = FakeResultsRepository()
        val useCase = AddResultUseCase(repo)
        val data = HousingPaymentDocument(documentType = "Квитанция ЖКУ")

        useCase(data, processedText = "clean text", source = ResultsRepository.SOURCE_AI)

        assertEquals(1, repo.addCalls.size)
        assertEquals(data, repo.addCalls.single().first)
        assertEquals(ResultsRepository.SOURCE_AI, repo.addCalls.single().second)
        assertEquals("clean text", repo.addCalls.single().third)
        assertEquals(listOf("clean text"), repo.markedTexts)
    }

    @Test
    fun addResultUseCase_addsResult_withoutMarking_whenProcessedTextNull() {
        val repo = FakeResultsRepository()
        val useCase = AddResultUseCase(repo)

        useCase(HousingPaymentDocument(documentType = "Квитанция ЖКУ"), processedText = null)

        assertEquals(1, repo.addCalls.size)
        assertTrue(repo.markedTexts.isEmpty())
    }

    @Test
    fun checkDuplicateUseCase_returnsRepositoryValue() {
        val repo = FakeResultsRepository(isDuplicate = true)
        val useCase = CheckDuplicateUseCase(repo)

        val result = useCase("same text")

        assertTrue(result)
        assertEquals("same text", repo.lastDuplicateQuery)
    }

    @Test
    fun clearResultsUseCase_callsRepositoryClear() {
        val repo = FakeResultsRepository()
        val useCase = ClearResultsUseCase(repo)

        useCase()

        assertTrue(repo.clearCalled)
    }

    @Test
    fun removeResultUseCase_callsRepositoryWithIndex() {
        val repo = FakeResultsRepository()
        val useCase = RemoveResultUseCase(repo)

        useCase(3)

        assertEquals(3, repo.lastRemovedIndex)
    }

    @Test
    fun observeResultsUseCase_returnsSameFlowInstance() {
        val repo = FakeResultsRepository()
        val useCase = ObserveResultsUseCase(repo)

        val flow = useCase()

        assertSame(repo.results, flow)
    }

    @Test
    fun parseWithLlmUseCase_delegatesToRepository() = runBlocking {
        val repo = FakeDocumentParseRepository(
            parseResult = ParseResult(housingDocument = HousingPaymentDocument(documentType = "Квитанция ЖКУ"), totalTokens = 10)
        )
        val useCase = ParseWithLlmUseCase(repo)

        val result = useCase("api-key", "clean text")

        assertEquals("api-key", repo.lastApiKey)
        assertEquals("clean text", repo.lastParseText)
        assertEquals(10, result.totalTokens)
    }

    @Test
    fun extractTextFromImageUseCase_delegatesWithForLlmFlag() = runBlocking {
        val repo = FakeDocumentParseRepository(extractedText = "parsed")
        val useCase = ExtractTextFromImageUseCase(repo)

        val result = useCase("file://photo.jpg", forLlm = false)

        assertEquals("file://photo.jpg", repo.lastExtractFileRef)
        assertFalse(repo.lastExtractForLlm)
        assertEquals("parsed", result)
    }

    private class FakeResultsRepository(
        private val isDuplicate: Boolean = false
    ) : ResultsRepository {
        override val results: StateFlow<List<HousingPaymentDocument>> = MutableStateFlow(emptyList())
        val addCalls = mutableListOf<Triple<HousingPaymentDocument, String, String?>>()
        val markedTexts = mutableListOf<String>()
        var lastDuplicateQuery: String? = null
        var clearCalled: Boolean = false
        var lastRemovedIndex: Int? = null

        override fun isTextAlreadyProcessed(cleanText: String): Boolean {
            lastDuplicateQuery = cleanText
            return isDuplicate
        }

        override fun markTextAsProcessed(cleanText: String) {
            markedTexts.add(cleanText)
        }

        override fun addResult(data: HousingPaymentDocument, source: String, processedText: String?) {
            addCalls.add(Triple(data, source, processedText))
            processedText?.let { markTextAsProcessed(it) }
        }

        override fun removeAt(index: Int) {
            lastRemovedIndex = index
        }

        override fun clearResults() {
            clearCalled = true
        }
    }

    private class FakeDocumentParseRepository(
        private val extractedText: String = "",
        private val parseResult: ParseResult = ParseResult(HousingPaymentDocument())
    ) : DocumentParseRepository {
        var lastExtractFileRef: String? = null
        var lastExtractForLlm: Boolean = true
        var lastApiKey: String? = null
        var lastParseText: String? = null

        override suspend fun extractText(fileRef: String, forLlm: Boolean): String {
            lastExtractFileRef = fileRef
            lastExtractForLlm = forLlm
            return extractedText
        }

        override suspend fun parseWithLlm(apiKey: String, cleanText: String): ParseResult {
            lastApiKey = apiKey
            lastParseText = cleanText
            return parseResult
        }
    }
}

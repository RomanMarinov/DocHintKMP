package app.romanmarinov.dochintkmp.shared.ios

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.data.remote.DocumentsShareGateway
import app.romanmarinov.dochintkmp.domain.repository.ResultsRepository
import app.romanmarinov.dochintkmp.domain.usecase.AddResultUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * KMP controller for Documents screen — same domain layer as Android.
 * Call [observeResults] to start receiving updates; [removeAt]/[clearResults] for user actions.
 */
class DocumentListController : KoinComponent {

    private val observeResultsUseCase: ObserveResultsUseCase by inject()
    private val removeResultUseCase: RemoveResultUseCase by inject()
    private val clearResultsUseCase: ClearResultsUseCase by inject()
    private val addResultUseCase: AddResultUseCase by inject()
    private val documentsShareGateway: DocumentsShareGateway by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestResults: List<MedicalData> = emptyList()

    /**
     * Start observing results. Callback is invoked on Main dispatcher.
     * @param onResults Called whenever the list changes
     */
    fun observeResults(onResults: (List<MedicalData>) -> Unit) {
        observeResultsUseCase()
            .onEach {
                latestResults = it
                onResults(it)
            }
            .catch { }
            .launchIn(scope)
    }

    fun removeAt(index: Int) {
        removeResultUseCase(index)
    }

    fun clearResults() {
        clearResultsUseCase()
    }

    fun sendDocuments(
        items: List<MedicalData>,
        onSuccess: (code: String, expiresAt: String) -> Unit,
        onError: (String?) -> Unit
    ) {
        scope.launch {
            runCatching { documentsShareGateway.sendDocuments(items) }
                .onSuccess { onSuccess(it.code, it.expiresAt) }
                .onFailure { onError(it.message) }
        }
    }

    fun receiveDocumentsByCode(
        code: String,
        onSuccess: (added: Int, skipped: Int) -> Unit,
        onError: (String?) -> Unit
    ) {
        scope.launch {
            runCatching {
                val incoming = documentsShareGateway.receiveDocumentsByCode(code)
                val existingReceived = latestResults.filter { it.source == ResultsRepository.SOURCE_SHARED }
                val uniqueItems = incoming.filter { candidate ->
                    existingReceived.none { it.sameAs(candidate) }
                }
                if (uniqueItems.isNotEmpty()) {
                    uniqueItems.forEach {
                        addResultUseCase(
                            data = it,
                            processedText = null,
                            source = ResultsRepository.SOURCE_SHARED
                        )
                    }
                }
                onSuccess(uniqueItems.size, incoming.size - uniqueItems.size)
            }.onFailure { onError(it.message) }
        }
    }
}

private fun MedicalData.sameAs(other: MedicalData): Boolean {
    return documentType == other.documentType &&
        institution == other.institution &&
        doctorName == other.doctorName &&
        analysisDate == other.analysisDate &&
        indicators == other.indicators
}

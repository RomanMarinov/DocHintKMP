package app.romanmarinov.dochintkmp.shared.ios

import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.usecase.ClearResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.ObserveResultsUseCase
import app.romanmarinov.dochintkmp.domain.usecase.RemoveResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Start observing results. Callback is invoked on Main dispatcher.
     * @param onResults Called whenever the list changes
     */
    fun observeResults(onResults: (List<MedicalData>) -> Unit) {
        observeResultsUseCase()
            .onEach { onResults(it) }
            .catch { }
            .launchIn(scope)
    }

    fun removeAt(index: Int) {
        removeResultUseCase(index)
    }

    fun clearResults() {
        clearResultsUseCase()
    }
}

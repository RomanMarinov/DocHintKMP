package app.romanmarinov.dochintkmp.presentation.documents_screen.model

import androidx.annotation.StringRes

sealed interface ResultsEffect {
    data class Error(
        val message: String?,
        @param:StringRes val messageRes: Int
    ) : ResultsEffect

    data class ImportSummary(val summary: ImportByCodeSummary) : ResultsEffect
}
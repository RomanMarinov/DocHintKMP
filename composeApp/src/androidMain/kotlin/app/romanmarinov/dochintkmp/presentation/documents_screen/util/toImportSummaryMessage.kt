package app.romanmarinov.dochintkmp.presentation.documents_screen.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.presentation.documents_screen.model.ImportByCodeSummary

@Composable
internal fun ImportByCodeSummary.toImportSummaryMessage(): String {
    return when {
        skipped > 0 && added > 0 -> stringResource(
            R.string.documents_import_summary_added_and_skipped,
            added,
            skipped
        )

        skipped > 0 -> stringResource(R.string.documents_import_summary_no_new)
        else -> stringResource(R.string.documents_import_summary_added, added)
    }
}
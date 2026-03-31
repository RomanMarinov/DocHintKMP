package app.romanmarinov.dochintkmp.presentation.documents_screen.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.EmptyState
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.ResultCard

@Composable
fun DocumentsTabContent(
    modifier: Modifier = Modifier,
    items: List<MedicalData>,
    selectionMode: Boolean,
    selectedIndices: Set<Int>,
    bottomInset: Dp,
    onToggleSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { i, _ -> i }) { index, data ->
            ResultCard(
                data = data,
                selectionMode = selectionMode,
                selected = index in selectedIndices,
                onToggleSelect = { onToggleSelect(index) },
                onDelete = { onDelete(index) }
            )
        }
    }
}


package app.romanmarinov.dochintkmp.presentation.documents_screen.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.MineTabEmptyState
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.MineTabResultCard

@Composable
fun DocumentsTabContent(
    modifier: Modifier = Modifier,
    items: List<HousingPaymentDocument>,
    selectionMode: Boolean,
    selectedIndices: Set<Int>,
    bottomInset: Dp,
    onToggleSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val gradientBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        gradientBottom
                    )
                )
            )
    ) {
        if (items.isEmpty()) {
            MineTabEmptyState(modifier = Modifier.fillMaxSize())
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = bottomInset
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(items, key = { i, _ -> i }) { index, data ->
                MineTabResultCard(
                    data = data,
                    selectionMode = selectionMode,
                    selected = index in selectedIndices,
                    onToggleSelect = { onToggleSelect(index) },
                    onDelete = { onDelete(index) }
                )
            }
        }
    }
}

package app.romanmarinov.dochintkmp.presentation.documents_screen.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.EmptyState
import app.romanmarinov.dochintkmp.presentation.documents_screen.components.ResultCard

@Composable
fun ReceivedTabContent(
    modifier: Modifier = Modifier,
    inputCode: String,
    networkBusy: Boolean,
    items: List<HousingPaymentDocument>,
    onInputCodeChange: (String) -> Unit,
    onClick: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = inputCode,
            onValueChange = onInputCodeChange,
            label = { Text(stringResource(R.string.documents_import_code_label)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = onClick,
                    enabled = inputCode.isNotBlank() && !networkBusy
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = stringResource(R.string.documents_import_button)
                    )
                }
            }
        )
    }

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
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { i, _ -> i }) { index, data ->
            ResultCard(
                data = data,
                selectionMode = false,
                selected = false,
                onToggleSelect = {},
                onDelete = { onDelete(index) }
            )
        }
    }
}


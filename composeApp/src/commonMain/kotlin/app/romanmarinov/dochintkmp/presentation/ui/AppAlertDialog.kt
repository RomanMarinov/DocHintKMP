package app.romanmarinov.dochintkmp.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppAlertDialog(
    visible: Boolean,
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String? = null,
    confirmTextColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmTextColor)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismissRequest) {
                    Text(it)
                }
            }
        }
    )
}


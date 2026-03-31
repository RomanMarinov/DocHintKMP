package app.romanmarinov.dochintkmp.presentation.documents_screen.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.romanmarinov.dochintkmp.R

@Composable
internal fun BoxScope.DocumentsShareFab(
    selectionMode: Boolean,
    selectedCount: Int,
    onClick: () -> Unit
) {
    val pickPhase = selectionMode && selectedCount == 0
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.content_desc_share)
            )
        },
        text = {
            Text(
                when {
                    !selectionMode -> stringResource(R.string.documents_action_send)
                    pickPhase -> stringResource(R.string.documents_fab_pick_first)
                    else -> stringResource(R.string.documents_share) + " ($selectedCount)"
                }
            )
        },
        expanded = true
    )
}
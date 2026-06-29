package app.romanmarinov.dochintkmp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.HousingServiceLine

@Composable
fun HousingBillDocumentContent(
    data: HousingPaymentDocument,
    modifier: Modifier = Modifier,
    showDocumentTypeChip: Boolean = false
) {
    Column(modifier = modifier) {
        if (showDocumentTypeChip) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.label_document_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            data.documentType,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        HousingPaymentSummary(data)

        data.institution?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(4.dp))
            HousingTextRow(stringResource(R.string.label_provider), it)
        }
        data.payerName?.takeIf { it.isNotBlank() && it != data.institution }?.let {
            HousingTextRow(stringResource(R.string.label_payer), it)
        }

        val compactFields = buildList {
            data.personalAccountNumber?.let { add(stringResource(R.string.label_personal_account) to it) }
            data.housingUtilitiesId?.let { add(stringResource(R.string.label_housing_utilities_id) to it) }
            data.paymentDocumentId?.let { add(stringResource(R.string.label_payment_document_id) to it) }
            data.documentDate?.let { add(stringResource(R.string.label_billing_period) to it) }
        }
        if (compactFields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HousingCompactFieldGrid(compactFields)
        }

        data.propertyAddress?.takeIf { it.isNotBlank() }?.let { address ->
            Spacer(modifier = Modifier.height(6.dp))
            HousingTextRow(stringResource(R.string.label_property_address), address)
        }

        val statChips = buildList {
            data.totalAreaSqm?.let { add(stringResource(R.string.label_total_area_short) to "$it м²") }
            data.livingAreaSqm?.let { add(stringResource(R.string.label_living_area_short) to "$it м²") }
            data.residentsCount?.let { add(stringResource(R.string.label_residents) to it) }
        }
        if (statChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HousingStatChips(statChips)
        }

        data.serviceLines?.takeIf { it.isNotEmpty() }?.let { services ->
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                stringResource(R.string.services_count, services.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    services.forEachIndexed { index, line ->
                        HousingServiceRow(line)
                        if (index < services.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HousingPaymentSummary(data: HousingPaymentDocument) {
    val amountDue = data.amountDueForPeriod?.let { "$it ₽" }
    val amountPaid = data.amountPaid?.let { "$it ₽" }
    val paymentDate = data.lastPaymentDate
    if (amountDue == null && amountPaid == null && paymentDate == null) return

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.label_amount_due),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
                Text(
                    amountDue ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (amountPaid != null || paymentDate != null) {
                Column(horizontalAlignment = Alignment.End) {
                    amountPaid?.let {
                        Text(
                            stringResource(R.string.label_amount_paid),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    paymentDate?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.label_payment_date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HousingTextRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HousingCompactFieldGrid(fields: List<Pair<String, String>>) {
    val rows = fields.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HousingStatChips(chips: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (label, value) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HousingServiceRow(line: HousingServiceLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            line.serviceReferenceLabel()?.let { ref ->
                Text(
                    ref,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "${line.amountToPay} ₽",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun HousingServiceLine.serviceReferenceLabel(): String? {
    val parts = buildList {
        volume?.let { add("$it ${unit.orEmpty()}".trim()) }
        tariff?.let { add("тариф $it") }
        volumeBasis?.let { add(it.name.lowercase()) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

fun HousingPaymentDocument.collapsedSubtitle(): String = buildList {
    documentDate?.takeIf { it.isNotBlank() }?.let { add(it) }
    amountDueForPeriod?.takeIf { it.isNotBlank() }?.let { add("$it ₽") }
}.joinToString(" · ")

fun HousingPaymentDocument.collapsedDetailLine(): String? =
    institution?.takeIf { it.isNotBlank() }
        ?: propertyAddress?.takeIf { it.isNotBlank() }

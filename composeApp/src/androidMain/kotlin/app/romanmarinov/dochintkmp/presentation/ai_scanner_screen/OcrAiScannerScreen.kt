package app.romanmarinov.dochintkmp.presentation.ai_scanner_screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.romanmarinov.dochintkmp.R
import app.romanmarinov.dochintkmp.presentation.ui.AppTopBar
import app.romanmarinov.dochintkmp.domain.model.FileType
import app.romanmarinov.dochintkmp.domain.model.MedicalData
import app.romanmarinov.dochintkmp.domain.model.ParseResult
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiContentState
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiErrorType
import app.romanmarinov.dochintkmp.presentation.ai_scanner_screen.model.OcrAiScannerEvent
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

@Composable
fun OcrAiScannerScreen(
    viewModel: OcrAiScannerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(OcrAiScannerEvent.RefreshKey)
    }

    val context = LocalContext.current
    val toastType = uiState.toastType
    if (toastType != null) {
        val message = stringResource(toastType.stringResId)
        LaunchedEffect(toastType) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: ""
            val fileType = when {
                mime.startsWith("image/") -> if (mime == "image/png") FileType.PNG else FileType.IMAGE
                mime == "application/pdf" -> FileType.PDF
                mime == "text/plain" -> FileType.TXT
                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> FileType.DOCX
                else -> FileType.IMAGE
            }
            viewModel.onEvent(OcrAiScannerEvent.SelectFile(it, fileType))
        }
    }

    val gradientBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.screen_ai_scanner))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            gradientBottom
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                AnimatedVisibility(visible = !uiState.hasSavedKey) {
                    AiApiKeyHintPanel(
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (uiState.selectedUri == null) {
                    val formats = stringResource(R.string.supported_formats)
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                pickFileLauncher.launch(
                                    arrayOf(
                                        "image/*", "image/jpeg", "image/png",
                                        "application/pdf",
                                        "text/plain",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    )
                                )
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.select_image),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.supported_formats),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        formats.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    val onOpenDocumentPreview: () -> Unit = {
                        uiState.selectedUri?.let { uri ->
                            openDocumentInSystemViewer(context, uri, uiState.selectedFileType)
                        }
                    }
                    val previewClickModifier = Modifier.clickable(onClick = onOpenDocumentPreview)

                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box {
                                when (uiState.selectedFileType) {
                                    FileType.IMAGE, FileType.PNG -> {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(uiState.selectedUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.content_desc_document_preview),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(216.dp)
                                                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                                .then(previewClickModifier),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FileType.PDF if uiState.pdfPreviewBitmap != null -> {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(uiState.pdfPreviewBitmap)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.content_desc_document_preview),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(216.dp)
                                                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                                .then(previewClickModifier),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> AiFileTypePreview(
                                        fileType = uiState.selectedFileType,
                                        onClick = onOpenDocumentPreview
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onEvent(OcrAiScannerEvent.ResetState) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.content_desc_remove),
                                            modifier = Modifier.padding(8.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val isSuccess = uiState.contentState is OcrAiContentState.Success
                    if (isSuccess) {
                        val success = uiState.contentState as OcrAiContentState.Success
                        Column {
                            AiSuccessPanel(result = success.parseResult)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.onEvent(OcrAiScannerEvent.SaveDocument) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    stringResource(R.string.adding_document_save),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(OcrAiScannerEvent.ProcessImage) },
                            enabled = uiState.contentState !is OcrAiContentState.Loading && uiState.hasSavedKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (uiState.contentState is OcrAiContentState.Loading) {
                                Text(stringResource(R.string.processing), style = MaterialTheme.typography.labelLarge)
                            } else {
                                Text(stringResource(R.string.recognize_document), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = uiState.contentState !is OcrAiContentState.Idle && uiState.contentState !is OcrAiContentState.Success,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    when (val s = uiState.contentState) {
                        is OcrAiContentState.Loading -> AiLoadingPanel()
                        is OcrAiContentState.Error -> AiErrorPanel(errorType = s.type, detailMessage = s.detailMessage)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AiApiKeyHintPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.add_api_key_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun AiLoadingPanel() {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            AiNumberedStep(1, stringResource(R.string.pipeline_ocr), done = true)
            AiNumberedStep(2, stringResource(R.string.pipeline_text_cleanup), done = true)
            AiNumberedStep(3, stringResource(R.string.pipeline_detect_type), done = true)
            AiNumberedStep(4, stringResource(R.string.pipeline_extract_llm), done = false)
        }
    }
}

@Composable
private fun AiNumberedStep(order: Int, label: String, done: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (done) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = order.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiSuccessPanel(result: ParseResult) {
    val data = result.data
    var indicatorsExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (indicatorsExpanded) 180f else 0f,
        animationSpec = tween(280),
        label = "arrow"
    )

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.data_extracted),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            data.documentType?.takeIf { it.isNotBlank() }?.let { type ->
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
                                type,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            AiAccentInfoRow(stringResource(R.string.label_institution), data.institution)
            
            val isHousingBill = data.documentType?.contains("квитанция", ignoreCase = true) == true ||
                                data.documentType?.contains("жку", ignoreCase = true) == true ||
                                data.documentType?.contains("жкх", ignoreCase = true) == true
            
            val doctorLabel = if (isHousingBill) 
                stringResource(R.string.label_payer)
            else 
                stringResource(R.string.label_doctor)
            
            val dateLabel = if (isHousingBill)
                stringResource(R.string.label_billing_period)
            else
                stringResource(R.string.label_analysis_date)
            
            AiAccentInfoRow(doctorLabel, data.doctorName)
            AiAccentInfoRow(dateLabel, data.analysisDate)

            data.indicators?.takeIf { it.isNotEmpty() }?.let { indicators ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { indicatorsExpanded = !indicatorsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.indicators_count, indicators.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(arrowRotation)
                    )
                }
                AnimatedVisibility(
                    visible = indicatorsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        indicators.forEach { ind ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ind.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        ind.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                ind.referenceRange?.takeIf { it.isNotBlank() }?.let { ref ->
                                    Text(
                                        ref,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            if (result.totalTokens > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            R.string.token_stats_format,
                            result.promptTokens,
                            result.completionTokens,
                            result.totalTokens
                        ),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiAccentInfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AiErrorPanel(errorType: OcrAiErrorType, detailMessage: String?) {
    val displayText = detailMessage?.takeIf { it.isNotBlank() }
        ?: stringResource(errorType.stringResId)
    OutlinedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AiFileTypePreview(fileType: FileType, onClick: (() -> Unit)? = null) {
    val (icon: ImageVector, label: String) = when (fileType) {
        FileType.PDF -> Icons.Outlined.PictureAsPdf to stringResource(R.string.file_type_pdf)
        FileType.TXT -> Icons.Outlined.Description to stringResource(R.string.file_type_txt)
        FileType.DOCX -> Icons.Outlined.Description to stringResource(R.string.file_type_docx)
        FileType.IMAGE -> Icons.Outlined.Description to stringResource(R.string.file_type_jpeg)
        FileType.PNG -> Icons.Outlined.Description to stringResource(R.string.file_type_png)
    }
    val iconTint = when (fileType) {
        FileType.PDF -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, Modifier.size(52.dp), tint = iconTint)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openDocumentInSystemViewer(context: Context, uri: Uri, fileType: FileType) {
    val mimeType = context.contentResolver.getType(uri)
        ?: when (fileType) {
            FileType.PDF -> "application/pdf"
            FileType.PNG -> "image/png"
            FileType.IMAGE -> "image/jpeg"
            FileType.TXT -> "text/plain"
            FileType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.error_no_viewer_for_document),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

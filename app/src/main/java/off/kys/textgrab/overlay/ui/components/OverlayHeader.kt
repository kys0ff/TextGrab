package off.kys.textgrab.overlay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.TessDataStore
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.core.model.OverlayCommand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayHeader(
    mode: ExtractionMode,
    ocrLanguage: OcrLanguage,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onToggleExpand: () -> Unit,
    onSwitchMode: (ExtractionMode) -> Unit,
    onSwitchLanguage: (OcrLanguage) -> Unit,
    onRescan: () -> Unit,
    onClose: () -> Unit,
    onOpenDownload: () -> Unit,
    onShowAutoModeWarning: () -> Unit,
    onShowMissingLanguage: (OcrLanguage) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(12.dp)
            .widthIn(max = 420.dp)
            .animateContentSize(spring(stiffness = 400f)),
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.6f)
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.common_app_label_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedContent(
                        targetState = mode,
                        label = "modeSubtitle",
                    ) { currentMode ->
                        Text(
                            text = stringResource(
                                if (currentMode == ExtractionMode.ACCESSIBILITY) {
                                    R.string.common_mode_label_accessibility
                                } else {
                                    R.string.common_mode_label_ocr
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                val chevronRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "chevronRotation",
                )
                FilledTonalIconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.overlay_action_button_collapse)
                        } else {
                            stringResource(R.string.overlay_action_button_expand)
                        },
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = chevronRotation },
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(
                    onClick = { OverlayBus.send(OverlayCommand.SetScrollMode(true)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = stringResource(R.string.overlay_action_button_scroll),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = onRescan, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.overlay_action_button_rescan),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = scheme.errorContainer,
                        contentColor = scheme.onErrorContainer,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.overlay_action_button_close),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = mode == ExtractionMode.ACCESSIBILITY,
                            onClick = { onSwitchMode(ExtractionMode.ACCESSIBILITY) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    Icons.Filled.Accessibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(stringResource(R.string.common_mode_label_accessibility)) },
                        )
                        SegmentedButton(
                            selected = mode == ExtractionMode.OCR,
                            onClick = { onSwitchMode(ExtractionMode.OCR) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(stringResource(R.string.common_mode_label_ocr)) },
                        )
                    }

                    AnimatedVisibility(visible = mode == ExtractionMode.OCR) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            LanguageSelector(
                                selected = ocrLanguage,
                                onSelect = { lang ->
                                    if (lang == OcrLanguage.AUTO) {
                                        onShowAutoModeWarning()
                                        return@LanguageSelector
                                    }
                                    val isInstalled =
                                        TessDataStore.hasAnyInstalled(context, lang.toTessCode())
                                    if (isInstalled) {
                                        onSwitchLanguage(lang)
                                    } else {
                                        onShowMissingLanguage(lang)
                                    }
                                },
                                onOpenDownload = onOpenDownload
                            )
                        }
                    }
                }
            }
        }
    }
}

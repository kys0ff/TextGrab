package off.kys.textgrab.overlay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.overlay.ui.components.InlineAlertDialog
import off.kys.textgrab.overlay.ui.components.OverlayActionBar
import off.kys.textgrab.overlay.ui.components.OverlayHeader
import off.kys.textgrab.overlay.ui.components.StatusCenter
import off.kys.textgrab.overlay.ui.components.TextBox
import off.kys.textgrab.overlay.ui.components.toDisplayString
import off.kys.textgrab.ui.theme.TextGrabTheme
import kotlin.math.roundToInt

private val MinVisibleEdge = 56.dp

private fun Float.coerceInSafe(a: Float, b: Float): Float {
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    return coerceIn(lo, hi)
}

private fun dragClamp(
    current: Float,
    delta: Float,
    basePosition: Float,
    panelSize: Float,
    containerSize: Float,
    minVisible: Float,
): Float {
    val minPos = -(panelSize - minVisible)
    val maxPos = containerSize - minVisible
    val minOffset = minPos - basePosition
    val maxOffset = maxPos - basePosition
    return (current + delta).coerceInSafe(minOffset, maxOffset)
}

@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel = viewModel(),
    onCopyAll: (List<String>, ExtractionMode) -> Unit,
    onOpenDownload: () -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OverlayUiEffect.CopyText -> onCopyAll(effect.texts, effect.mode)
                OverlayUiEffect.CloseOverlay -> onClose()
                OverlayUiEffect.NavigateToDownloads -> onOpenDownload()
            }
        }
    }

    // Window position state for dragging the header control box
    var headerOffsetX by remember { mutableFloatStateOf(0f) }
    var headerOffsetY by remember { mutableFloatStateOf(0f) }
    var headerSize by remember { mutableStateOf(IntSize.Zero) }

    // Window position state for dragging the bottom action bar
    var actionBarOffsetX by remember { mutableFloatStateOf(0f) }
    var actionBarOffsetY by remember { mutableFloatStateOf(0f) }
    var actionBarSize by remember { mutableStateOf(IntSize.Zero) }

    BackHandler {
        viewModel.onEvent(OverlayUiEvent.Close)
    }

    TextGrabTheme {
        AnimatedContent(
            targetState = state.isScrollMode,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.95f))
            },
            label = "scrollModeTransition",
            modifier = Modifier.animateContentSize()
        ) { scrolling ->
            BoxWithConstraints(
                modifier = if (scrolling) Modifier.wrapContentSize() else Modifier.fillMaxSize()
            ) {
                val density = LocalDensity.current
                val containerWidthPx = with(density) { maxWidth.toPx() }
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val minVisiblePx = with(density) { MinVisibleEdge.toPx() }

                if (scrolling) {
                    Surface(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 12.dp,
                        onClick = { viewModel.onEvent(OverlayUiEvent.SetScrollMode(false)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_document_scanner),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.overlay_action_button_scroll_done),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.55f),
                                    0.15f to Color.Black.copy(alpha = 0.40f),
                                    0.85f to Color.Black.copy(alpha = 0.40f),
                                    1f to Color.Black.copy(alpha = 0.55f),
                                ),
                            )
                            .pointerInput(Unit) { detectTapGestures(onTap = { /* CONSUMED */ }) },
                    )

                    // Text Bounding Boxes
                    if (state.status is OverlayStatus.Ready) state.elements.forEach { element ->
                        TextBox(
                            element = element,
                            selected = state.selectedIds.contains(element.id),
                            multiSelect = state.multiSelect,
                            widthDp = with(density) { element.width.toDp() },
                            heightDp = with(density) { element.height.toDp() },
                        ) {
                            viewModel.onEvent(OverlayUiEvent.ToggleElementSelection(element.id))
                        }
                    }

                    // Draggable & Collapsible Header Control Container
                    OverlayHeader(
                        mode = state.mode,
                        ocrLanguage = state.ocrLanguage,
                        isExpanded = state.isExpanded,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    headerOffsetX.roundToInt(),
                                    headerOffsetY.roundToInt()
                                )
                            }
                            .align(Alignment.TopCenter)
                            .onSizeChanged { headerSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val baseLeft = (containerWidthPx - headerSize.width) / 2f
                                    val baseTop = 0f
                                    headerOffsetX = dragClamp(
                                        current = headerOffsetX,
                                        delta = dragAmount.x,
                                        basePosition = baseLeft,
                                        panelSize = headerSize.width.toFloat(),
                                        containerSize = containerWidthPx,
                                        minVisible = minVisiblePx,
                                    )
                                    headerOffsetY = dragClamp(
                                        current = headerOffsetY,
                                        delta = dragAmount.y,
                                        basePosition = baseTop,
                                        panelSize = headerSize.height.toFloat(),
                                        containerSize = containerHeightPx,
                                        minVisible = minVisiblePx,
                                    )
                                }
                            },
                        onToggleExpand = { viewModel.onEvent(OverlayUiEvent.ToggleHeaderExpansion) },
                        onSwitchMode = { viewModel.onEvent(OverlayUiEvent.SwitchMode(it)) },
                        onSwitchLanguage = { viewModel.onEvent(OverlayUiEvent.SwitchLanguage(it)) },
                        onRescan = { viewModel.onEvent(OverlayUiEvent.Rescan) },
                        onClose = { viewModel.onEvent(OverlayUiEvent.Close) },
                        onOpenDownload = { viewModel.onEvent(OverlayUiEvent.OpenDownload) },
                        onShowAutoModeWarning = { viewModel.showAutoModeWarningDialog() },
                        onShowMissingLanguage = { viewModel.showMissingLanguageDialog(it) }
                    )

                    StatusCenter(
                        status = state.status,
                        mode = state.mode,
                        modifier = Modifier.align(Alignment.Center),
                        onSwitchToOcr = { viewModel.onEvent(OverlayUiEvent.SwitchMode(ExtractionMode.OCR)) },
                        onRescan = { viewModel.onEvent(OverlayUiEvent.Rescan) },
                        onOpenDownload = { viewModel.onEvent(OverlayUiEvent.OpenDownload) },
                    )

                    AnimatedVisibility(
                        visible = state.elements.isNotEmpty(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        OverlayActionBar(
                            multiSelect = state.multiSelect,
                            selectedCount = state.selectedIds.size,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        actionBarOffsetX.roundToInt(),
                                        actionBarOffsetY.roundToInt()
                                    )
                                }
                                .onSizeChanged { actionBarSize = it }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val baseLeft = (containerWidthPx - actionBarSize.width) / 2f
                                        val baseTop = containerHeightPx - actionBarSize.height
                                        actionBarOffsetX = dragClamp(
                                            current = actionBarOffsetX,
                                            delta = dragAmount.x,
                                            basePosition = baseLeft,
                                            panelSize = actionBarSize.width.toFloat(),
                                            containerSize = containerWidthPx,
                                            minVisible = minVisiblePx,
                                        )
                                        actionBarOffsetY = dragClamp(
                                            current = actionBarOffsetY,
                                            delta = dragAmount.y,
                                            basePosition = baseTop,
                                            panelSize = actionBarSize.height.toFloat(),
                                            containerSize = containerHeightPx,
                                            minVisible = minVisiblePx,
                                        )
                                    }
                                },
                            onToggleSelect = { viewModel.onEvent(OverlayUiEvent.ToggleMultiSelect) },
                            onCopySelected = { viewModel.onEvent(OverlayUiEvent.CopySelected) },
                            onCopyAll = { viewModel.onEvent(OverlayUiEvent.CopyAll) },
                        )
                    }

                    if (state.autoModeWarningDialog) {
                        InlineAlertDialog(
                            onDismissRequest = { viewModel.onEvent(OverlayUiEvent.DismissAutoModeWarningDialog) },
                            title = { Text(stringResource(R.string.ocr_package_label_auto_warning_title)) },
                            text = { Text(stringResource(R.string.ocr_package_label_auto_warning_desc)) },
                            confirmButton = {
                                Button(onClick = { viewModel.onEvent(OverlayUiEvent.ConfirmAutoModeWarning) }) {
                                    Text(stringResource(R.string.common_action_button_grant))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.onEvent(OverlayUiEvent.DismissAutoModeWarningDialog) }) {
                                    Text(stringResource(R.string.common_action_button_cancel))
                                }
                            }
                        )
                    }

                    state.missingLanguageDialog?.let { lang ->
                        InlineAlertDialog(
                            onDismissRequest = { viewModel.onEvent(OverlayUiEvent.DismissMissingLanguageDialog) },
                            title = { Text(stringResource(R.string.ocr_package_label_missing_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.ocr_package_label_missing_desc,
                                        stringResource(lang.toDisplayString())
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.onEvent(OverlayUiEvent.DismissMissingLanguageDialog)
                                    viewModel.onEvent(OverlayUiEvent.OpenDownload)
                                }) {
                                    Text(stringResource(R.string.common_action_button_download))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.onEvent(OverlayUiEvent.DismissMissingLanguageDialog) }) {
                                    Text(stringResource(R.string.common_action_button_cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}



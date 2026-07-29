package off.kys.textgrab.overlay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.GrabbedText
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.core.model.OverlayStatus
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.ui.theme.TextGrabTheme
import kotlin.math.roundToInt

/**
 * How much of a draggable panel (header / action bar) must always remain
 * visible on screen. This lets the user shove a panel toward an edge to get
 * it out of the way without ever losing it off-screen entirely.
 */
private val MinVisibleEdge = 56.dp

/**
 * Coerces [this] between [a] and [b] regardless of which one is smaller,
 * so we never crash if a panel happens to be bigger than its container.
 */
private fun Float.coerceInSafe(a: Float, b: Float): Float {
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    return coerceIn(lo, hi)
}

/**
 * Computes a new drag offset along a single axis, keeping at least
 * [minVisible] px of the panel inside the [containerSize] px bounds.
 *
 * @param current current offset value on this axis
 * @param delta incoming drag delta on this axis
 * @param basePosition the panel's un-offset position on this axis (e.g. from alignment)
 * @param panelSize the panel's measured size on this axis
 * @param containerSize the container's size on this axis
 * @param minVisible minimum px of the panel that must stay on-screen
 */
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
    onCopyAll: (List<String>, ExtractionMode) -> Unit,
    onSwitchMode: (ExtractionMode) -> Unit,
    onSwitchLanguage: (OcrLanguage) -> Unit,
    onRescan: () -> Unit,
    onClose: () -> Unit,
) {
    val elements by OverlayBus.elements.collectAsState()
    val mode by OverlayBus.mode.collectAsState()
    val ocrLanguage by OverlayBus.ocrLanguage.collectAsState()
    val status by OverlayBus.status.collectAsState()

    var multiSelect by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

    // Window position state for dragging the header control box
    var headerOffsetX by remember { mutableFloatStateOf(0f) }
    var headerOffsetY by remember { mutableFloatStateOf(0f) }
    var headerSize by remember { mutableStateOf(IntSize.Zero) }
    var isExpanded by remember { mutableStateOf(true) }

    // Window position state for dragging the bottom action bar
    var actionBarOffsetX by remember { mutableFloatStateOf(0f) }
    var actionBarOffsetY by remember { mutableFloatStateOf(0f) }
    var actionBarSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(elements) { selected.clear() }

    TextGrabTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val minVisiblePx = with(density) { MinVisibleEdge.toPx() }

            // Dim scrim with a soft vertical gradient so content near the
            // header/action bar reads clearly without a flat, harsh overlay
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
                    .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) },
            )

            // Text Bounding Boxes
            elements.forEach { element ->
                TextBox(
                    element = element,
                    selected = selected.contains(element.id),
                    multiSelect = multiSelect,
                    widthDp = with(density) { element.width.toDp() },
                    heightDp = with(density) { element.height.toDp() },
                    onSelect = {
                        // A single touch now starts/extends selection instead
                        // of requiring a long press: touching an item enters
                        // multi-select (if not already active) and toggles it.
                        if (!multiSelect) multiSelect = true
                        if (selected.contains(element.id)) {
                            selected.remove(element.id)
                        } else {
                            selected.add(element.id)
                        }
                    },
                )
            }

            // Draggable & Collapsible Header Control Container
            OverlayHeader(
                mode = mode,
                ocrLanguage = ocrLanguage,
                isExpanded = isExpanded,
                modifier = Modifier
                    .offset { IntOffset(headerOffsetX.roundToInt(), headerOffsetY.roundToInt()) }
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
                onToggleExpand = { isExpanded = !isExpanded },
                onSwitchMode = onSwitchMode,
                onSwitchLanguage = onSwitchLanguage,
                onRescan = onRescan,
                onClose = onClose,
            )

            StatusCenter(
                status = status,
                mode = mode,
                modifier = Modifier.align(Alignment.Center),
                onSwitchToOcr = { onSwitchMode(ExtractionMode.OCR) },
                onRescan = onRescan,
            )

            AnimatedVisibility(
                visible = elements.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OverlayActionBar(
                    multiSelect = multiSelect,
                    selectedCount = selected.size,
                    modifier = Modifier
                        .offset {
                            IntOffset(actionBarOffsetX.roundToInt(), actionBarOffsetY.roundToInt())
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
                    onToggleSelect = {
                        multiSelect = !multiSelect
                        if (!multiSelect) selected.clear()
                    },
                    onCopySelected = {
                        val texts = elements.filter { selected.contains(it.id) }.map { it.text }
                        onCopyAll(texts, mode)
                    },
                    onCopyAll = { onCopyAll(elements.map { it.text }, mode) },
                )
            }
        }
    }
}

@Composable
private fun TextBox(
    element: GrabbedText,
    selected: Boolean,
    multiSelect: Boolean,
    widthDp: Dp,
    heightDp: Dp,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    val borderColor = if (selected) scheme.primary else scheme.primary.copy(alpha = 0.55f)
    val fillColor = if (selected) {
        scheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        scheme.surfaceContainerHighest.copy(alpha = 0.90f)
    }
    val textColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface

    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        label = "textBoxBorderWidth",
    )

    Box(
        Modifier
            .offset { IntOffset(element.left, element.top) }
            .size(width = widthDp.coerceAtLeast(28.dp), height = heightDp.coerceAtLeast(22.dp))
            .shadow(
                elevation = if (selected) 4.dp else 1.dp,
                shape = RoundedCornerShape(8.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(8.dp))
            .background(fillColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(element.id) {
                // A simple touch now starts/extends the selection directly,
                // no long press required.
                detectTapGestures(onTap = { onSelect() })
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (element.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            Text(
                text = element.text,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = textColor,
            )
        }

        // Small selection indicator dot, only shown while multi-selecting,
        // mirrors the familiar Google Photos / Files selection affordance
        if (multiSelect) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else scheme.surface.copy(alpha = 0.8f))
                    .border(
                        width = 1.dp,
                        color = if (selected) scheme.primary else scheme.outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayHeader(
    mode: ExtractionMode,
    ocrLanguage: OcrLanguage,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onToggleExpand: () -> Unit,
    onSwitchMode: (ExtractionMode) -> Unit,
    onSwitchLanguage: (OcrLanguage) -> Unit,
    onRescan: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

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
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.6f)
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Drag Bar Handle Icon Visual Cue
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // App icon in a tonal container, matching Google's "avatar chip" pattern
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
                        stringResource(R.string.app_name),
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
                                    R.string.mode_accessibility
                                } else {
                                    R.string.mode_ocr
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                FilledTonalIconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.overlay_collapse)
                        } else {
                            stringResource(R.string.overlay_expand)
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = onRescan, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.overlay_rescan),
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
                        contentDescription = stringResource(R.string.overlay_close),
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
                            label = { Text(stringResource(R.string.mode_accessibility)) },
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
                            label = { Text(stringResource(R.string.mode_ocr)) },
                        )
                    }

                    AnimatedVisibility(visible = mode == ExtractionMode.OCR) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = ocrLanguage == OcrLanguage.LATIN,
                                    onClick = { onSwitchLanguage(OcrLanguage.LATIN) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                    label = { Text(stringResource(R.string.ocr_lang_latin)) },
                                )
                                SegmentedButton(
                                    selected = ocrLanguage == OcrLanguage.ARABIC,
                                    onClick = { onSwitchLanguage(OcrLanguage.ARABIC) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                    label = { Text(stringResource(R.string.ocr_lang_arabic)) },
                                )
                                SegmentedButton(
                                    selected = ocrLanguage == OcrLanguage.BOTH,
                                    onClick = { onSwitchLanguage(OcrLanguage.BOTH) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                    label = { Text(stringResource(R.string.ocr_lang_both)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayActionBar(
    multiSelect: Boolean,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onToggleSelect: () -> Unit,
    onCopySelected: () -> Unit,
    onCopyAll: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.6f)
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BadgedBox(
                badge = {
                    if (multiSelect && selectedCount > 0) {
                        Badge(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ) { Text("$selectedCount") }
                    }
                },
            ) {
                FilterChip(
                    selected = multiSelect,
                    onClick = onToggleSelect,
                    label = { Text(stringResource(R.string.overlay_select)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = scheme.secondaryContainer,
                        selectedLabelColor = scheme.onSecondaryContainer,
                        selectedLeadingIconColor = scheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = multiSelect,
                        borderColor = scheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                    ),
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = multiSelect,
                transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                label = "copyButton",
            ) { isMultiSelect ->
                if (isMultiSelect) {
                    Button(
                        onClick = onCopySelected,
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.overlay_copy_selected, selectedCount))
                    }
                } else {
                    Button(
                        onClick = onCopyAll,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            Icons.Filled.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.overlay_copy_all))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCenter(
    status: OverlayStatus,
    mode: ExtractionMode,
    modifier: Modifier = Modifier,
    onSwitchToOcr: () -> Unit,
    onRescan: () -> Unit,
) {
    when (status) {
        OverlayStatus.Scanning -> InfoCard(modifier) {
            CircularProgressIndicator(
                Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.overlay_scanning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        OverlayStatus.Empty -> InfoCard(modifier) {
            IllustrationBadge(icon = Icons.Filled.SearchOff)
            Spacer(Modifier.height(16.dp))
            val message = if (mode == ExtractionMode.ACCESSIBILITY) {
                stringResource(R.string.overlay_empty_accessibility)
            } else {
                stringResource(R.string.overlay_empty_ocr)
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (mode == ExtractionMode.ACCESSIBILITY) {
                FilledTonalButton(
                    onClick = onSwitchToOcr,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mode_ocr))
                }
            } else {
                FilledTonalButton(
                    onClick = onRescan,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.overlay_rescan))
                }
            }
        }

        is OverlayStatus.Error -> InfoCard(modifier) {
            IllustrationBadge(icon = Icons.Filled.ErrorOutline, error = true)
            Spacer(Modifier.height(16.dp))
            Text(
                status.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        OverlayStatus.Idle, is OverlayStatus.Ready -> Unit
    }
}

@Composable
private fun IllustrationBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (error) scheme.errorContainer else scheme.secondaryContainer
    val content = if (error) scheme.onErrorContainer else scheme.onSecondaryContainer
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
        ) {
            Column(
                Modifier.padding(28.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content() }
        }
    }
}
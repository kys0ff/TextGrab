package off.kys.textgrab.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Material 3 setup / home screen: a collapsing top bar, a single grouped card
 * with permission rows, tile instructions and the copy-history log, plus a
 * "Scan now" FAB shown once everything is ready.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissions: PermissionUiState,
    history: List<HistoryEntry>,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onClearHistory: () -> Unit,
    onCopyHistory: (String) -> Unit,
    onScanNow: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ready = permissions.accessibility && permissions.overlay

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = ready) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.scan_now)) },
                    icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                    onClick = onScanNow,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.setup_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // All setup rows grouped into one card — far more compact than three
            // separate elevated cards, and reads as a single coherent checklist.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column {
                        PermissionRow(
                            icon = Icons.Filled.Accessibility,
                            title = stringResource(R.string.perm_accessibility_title),
                            description = stringResource(R.string.perm_accessibility_desc),
                            granted = permissions.accessibility,
                            actionLabel = stringResource(R.string.action_open_settings),
                            onAction = onOpenAccessibility,
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            )
                        )
                        PermissionRow(
                            icon = Icons.Filled.Layers,
                            title = stringResource(R.string.perm_overlay_title),
                            description = stringResource(R.string.perm_overlay_desc),
                            granted = permissions.overlay,
                            actionLabel = stringResource(R.string.action_grant),
                            onAction = onOpenOverlay,
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            )
                        )
                        // MediaProjection needs no persistent grant — consent is
                        // requested per use — so this row is purely informational.
                        PermissionRow(
                            icon = Icons.Filled.Image,
                            title = stringResource(R.string.perm_projection_title),
                            description = stringResource(R.string.perm_projection_desc),
                            granted = true,
                            isInfoOnly = true,
                            actionLabel = null,
                            onAction = {},
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        leadingContent = { IconBadge(Icons.Filled.Widgets, tonal = false) },
                        headlineContent = {
                            Text(
                                stringResource(R.string.tile_setup_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.tile_setup_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (history.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Text(stringResource(R.string.history_clear))
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column {
                            history.forEachIndexed { index, entry ->
                                HistoryRow(entry = entry, onCopy = { onCopyHistory(entry.text) })
                                if (index != history.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
    isInfoOnly: Boolean = false,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { IconBadge(icon, tonal = true, active = granted || isInfoOnly) },
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                )

                when {
                    isInfoOnly -> Unit
                    actionLabel != null && !granted -> {
                        TextButton(
                            onClick = onAction,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        },
        trailingContent = {
            when {
                isInfoOnly -> Unit
                granted -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.status_granted),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    )
}

@Composable
private fun IconBadge(icon: ImageVector, tonal: Boolean, active: Boolean = true) {
    val background = if (tonal && active) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (tonal) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
    }
    val tint = if (tonal && active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (tonal) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = CircleShape,
        color = background,
        modifier = Modifier.size(36.dp),
    ) {
        Box(icon, tint)
    }
}

@Composable
private fun Box(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onCopy: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = {
            Text(
                entry.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                "${sourceLabel(entry.source)} · ${formatTime(entry.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.copied_toast)
                )
            }
        },
    )
}

@Composable
private fun sourceLabel(source: ExtractionMode): String = stringResource(
    when (source) {
        ExtractionMode.ACCESSIBILITY -> R.string.mode_accessibility
        ExtractionMode.OCR -> R.string.mode_ocr
    },
)

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
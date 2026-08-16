package off.kys.textgrab.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.ui.screens.ocr.OcrPackageScreen
import off.kys.textgrab.ui.theme.TextGrabTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class MainScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<MainViewModel>()
        val state by viewModel.state.collectAsState()

        val permissionManager = koinInject<PermissionManager>()
        val navigator = LocalNavigator.currentOrThrow

        MainScreenContent(
            state = state,
            onEvent = viewModel::onEvent,
            onOpenAccessibility = { permissionManager.openAccessibilitySettings() },
            onOpenOverlay = { permissionManager.openOverlaySettings() },
            onOpenNotifications = { permissionManager.openNotificationsSettings() },
            onNavigateToOcr = { navigator.push(OcrPackageScreen()) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigateToOcr: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
    }

    LifecycleResumeEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
        onPauseOrDispose {}
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ready = state.permissions.accessibility && state.permissions.overlay
    val grantedPermissions = listOf(
        state.permissions.accessibility,
        state.permissions.overlay,
        state.permissions.notifications
    ).count { it }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.common_app_label_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = ready,
                enter = expandIn(expandFrom = Alignment.Center) + fadeIn(),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        OverlayBus.send(OverlayCommand.Trigger(ExtractionMode.ACCESSIBILITY))
                    },
                    shape = CircleShape,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_document_scanner),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.main_action_button_scan),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ReadinessHero(
                    granted = grantedPermissions,
                    ready = ready
                )
            }

            item {
                SectionHeader(title = stringResource(R.string.permission_accessibility_label_title))
            }

            item {
                PermissionGrid(
                    state = state,
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenOverlay = onOpenOverlay,
                    onOpenNotifications = onOpenNotifications,
                    onOcr = onNavigateToOcr
                )
            }

            item {
                TileSetupCompact()
            }

            item {
                HistorySection(
                    history = state.history,
                    onClear = { onEvent(MainEvent.ClearHistory) },
                    onCopy = { onEvent(MainEvent.OnHistoryCopy(it)) }
                )
            }
        }

        if (state.showClearHistoryConfirmation) {
            ClearHistoryDialog(
                onConfirm = { onEvent(MainEvent.ConfirmClearHistory) },
                onDismiss = { onEvent(MainEvent.DismissClearHistoryDialog) }
            )
        }
    }
}

@Composable
private fun ReadinessHero(
    granted: Int,
    total: Int = 3,
    ready: Boolean
) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else granted.toFloat() / total,
        animationSpec = tween(600),
        label = "readinessProgress"
    )

    val gradient = if (ready) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }

    val contentColor = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIcon(ready = ready, progress = progress, granted = granted, total = total)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (ready) R.string.main_label_readiness_ready_title
                            else R.string.main_label_readiness_pending_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = stringResource(
                            if (ready) R.string.main_label_readiness_ready_desc
                            else R.string.main_label_readiness_pending_desc
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    ready: Boolean,
    progress: Float,
    granted: Int,
    total: Int
) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = ready,
            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
            label = "readinessIcon"
        ) { isReady ->
            if (isReady) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rocket_launch),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = stringResource(R.string.main_label_readiness_progress, granted, total),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun PermissionGrid(
    state: MainState,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOcr: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionItem(
            icon = painterResource(R.drawable.ic_accessibility),
            title = stringResource(R.string.permission_accessibility_label_title),
            granted = state.permissions.accessibility,
            onAction = onOpenAccessibility
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_layers),
            title = stringResource(R.string.permission_overlay_label_title),
            granted = state.permissions.overlay,
            onAction = onOpenOverlay
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_notifications),
            title = stringResource(R.string.permission_notifications_label_title),
            granted = state.permissions.notifications,
            onAction = onOpenNotifications
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_image),
            title = stringResource(R.string.permission_projection_label_title),
            granted = true,
            onAction = onOcr,
            isInfo = true
        )
    }
}

@Composable
private fun PermissionItem(
    icon: Painter,
    title: String,
    granted: Boolean,
    onAction: () -> Unit,
    isInfo: Boolean = false
) {
    val containerColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "permContainer"
    )

    Surface(
        onClick = onAction,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (granted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (isInfo) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else if (granted) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.common_action_button_grant),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TileSetupCompact() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_widgets),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.tile_setup_label_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.tile_setup_label_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<HistoryEntry>,
    onClear: () -> Unit,
    onCopy: (HistoryEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = stringResource(R.string.history_label_title))
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(R.string.history_action_button_clear),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (history.isEmpty()) {
            EmptyHistoryState()
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    history.take(10).forEach { entry ->
                        HistoryItem(entry = entry, onCopy = { onCopy(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(entry: HistoryEntry, onCopy: () -> Unit) {
    var justCopied by remember { mutableStateOf(false) }
    val dateFormat = stringResource(R.string.common_date_format_history)
    val separator = stringResource(R.string.common_label_separator_bullet)

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1200.milliseconds)
            justCopied = false
        }
    }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${sourceLabel(entry.source)}$separator${formatTime(entry.timestamp, dateFormat)}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            FilledTonalIconButton(
                onClick = {
                    onCopy()
                    justCopied = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = if (justCopied) painterResource(R.drawable.ic_check) else painterResource(R.drawable.ic_content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
private fun EmptyHistoryState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_inventory_2),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_label_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.history_action_button_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_action_button_cancel))
            }
        },
        title = { Text(stringResource(R.string.history_clear_confirm_title)) },
        text = { Text(stringResource(R.string.history_clear_confirm_desc)) }
    )
}

@Composable
private fun sourceLabel(source: ExtractionMode): String =
    stringResource(
        id = when (source) {
            ExtractionMode.ACCESSIBILITY -> R.string.common_mode_label_accessibility
            ExtractionMode.OCR -> R.string.common_mode_label_ocr
        }
    )

private fun formatTime(timestamp: Long, format: String): String =
    SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    TextGrabTheme {
        MainScreenContent(
            state = MainState(
                history = listOf(
                    HistoryEntry(1L, "Sample extracted text 1", System.currentTimeMillis(), ExtractionMode.ACCESSIBILITY),
                    HistoryEntry(2L, "Sample extracted text 2", System.currentTimeMillis() - 1000000, ExtractionMode.OCR)
                ),
                permissions = PermissionUiState(accessibility = true, overlay = false, notifications = true)
            ),
            onEvent = {},
            onOpenAccessibility = {},
            onOpenOverlay = {},
            onOpenNotifications = {},
            onNavigateToOcr = {}
        )
    }
}

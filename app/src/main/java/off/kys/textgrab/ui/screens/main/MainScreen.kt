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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.core.model.OverlayCommand
import off.kys.textgrab.core.permission.PermissionManager
import off.kys.textgrab.overlay.OverlayBus
import off.kys.textgrab.ui.screens.ocr.OcrPackageScreen
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
            permissionManager = permissionManager,
            navigator = navigator
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    permissionManager: PermissionManager,
    navigator: Navigator
) {
    LaunchedEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
    }

    LifecycleResumeEffect(Unit) {
        onEvent(MainEvent.RefreshPermissions)
        onPauseOrDispose {}
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
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
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.scan_now),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReadinessSummary(
                    granted = grantedPermissions,
                    ready = ready
                )
            }

            item { SetupIntro() }

            item {
                PermissionSection(
                    accessibilityGranted = state.permissions.accessibility,
                    overlayGranted = state.permissions.overlay,
                    notificationsGranted = state.permissions.notifications,
                    onAccessibility = permissionManager::openAccessibilitySettings,
                    onOverlay = permissionManager::openOverlaySettings,
                    onNotifications = permissionManager::openNotificationsSettings,
                    onOcr = { navigator.push(OcrPackageScreen()) }
                )
            }

            item { TileSetupCard() }

            item {
                HistoryHeader(
                    hasHistory = state.history.isNotEmpty(),
                    onClear = {
                        onEvent(
                            MainEvent.ClearHistory
                        )
                    }
                )
            }

            if (state.history.isEmpty()) {
                item { EmptyHistory() }
            } else {
                item {
                    HistoryCard(
                        history = state.history,
                        onCopy = {

                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupIntro() {
    Text(
        text = stringResource(R.string.setup_intro),
        modifier = Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReadinessSummary(
    granted: Int,
    total: Int = 3,
    ready: Boolean
) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) {
            0f
        } else {
            granted.toFloat() / total
        },
        animationSpec = tween(450),
        label = "readinessProgress"
    )

    val containerColor by animateColorAsState(
        targetValue =
            if (ready) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = tween(300),
        label = "readinessContainer"
    )

    val contentColor =
        if (ready) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = ready,
                    transitionSpec = {
                        (scaleIn() + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                    },
                    label = "readinessIcon"
                ) { isReady ->
                    if (isReady) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                    } else {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 4.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                                    .copy(alpha = 0.35f)
                            )

                            Text(
                                text = "$granted/$total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (ready) {
                            R.string.readiness_ready_title
                        } else {
                            R.string.readiness_pending_title
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(
                        if (ready) {
                            R.string.readiness_ready_desc
                        } else {
                            R.string.readiness_pending_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ready) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                            .copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionSection(
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    notificationsGranted: Boolean,
    onAccessibility: () -> Unit,
    onOverlay: () -> Unit,
    onNotifications: () -> Unit,
    onOcr: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionCard(
            icon = Icons.Default.Accessibility,
            title = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted = accessibilityGranted,
            actionLabel = stringResource(R.string.action_open_settings),
            onAction = onAccessibility
        )

        PermissionCard(
            icon = Icons.Default.Layers,
            title = stringResource(R.string.perm_overlay_title),
            description = stringResource(R.string.perm_overlay_desc),
            granted = overlayGranted,
            actionLabel = stringResource(R.string.action_grant),
            onAction = onOverlay
        )

        PermissionCard(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.perm_notifications_title),
            description = stringResource(R.string.perm_notifications_desc),
            granted = notificationsGranted,
            actionLabel = stringResource(R.string.perm_notifications_action),
            onAction = onNotifications
        )

        PermissionCard(
            icon = Icons.Default.Image,
            title = stringResource(R.string.perm_projection_title),
            description = stringResource(R.string.perm_projection_desc),
            granted = true,
            actionLabel = stringResource(R.string.ocr_download_title),
            onAction = onOcr,
            infoOnly = true
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    infoOnly: Boolean = false
) {
    val containerColor by animateColorAsState(
        targetValue =
            when {
                infoOnly -> MaterialTheme.colorScheme.surfaceContainer
                granted -> MaterialTheme.colorScheme.surfaceContainerLow

                else -> MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = tween(180),
        label = "permissionContainer"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp,
                    end = 12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            PermissionIcon(
                icon = icon,
                granted = granted
            )

            Spacer(modifier = Modifier.size(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!granted || infoOnly) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onAction) {
                        Text(
                            text = actionLabel,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (infoOnly) {
                            Spacer(modifier = Modifier.size(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (granted && !infoOnly) {
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionIcon(
    icon: ImageVector,
    granted: Boolean
) {
    val backgroundColor = if (granted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val iconColor = if (granted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.size(48.dp),
        shape = MaterialTheme.shapes.large,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TileSetupCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.onSecondaryContainer
                    .copy(alpha = 0.10f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tile_setup_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(R.string.tile_setup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSecondaryContainer
                            .copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    hasHistory: Boolean,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (hasHistory) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = hasHistory,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.history_clear),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    history: List<HistoryEntry>,
    onCopy: (HistoryEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            history.forEachIndexed { index, entry ->
                HistoryRow(
                    entry = entry,
                    onCopy = { onCopy(entry) }
                )

                if (index < history.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onCopy: () -> Unit
) {
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1200.milliseconds)
            justCopied = false
        }
    }

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${sourceLabel(entry.source)} · ${formatTime(entry.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            FilledTonalIconButton(
                onClick = {
                    onCopy()
                    justCopied = true
                },
                modifier = Modifier.size(40.dp)
            ) {
                AnimatedContent(
                    targetState = justCopied,
                    transitionSpec = {
                        (scaleIn() + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                    },
                    label = "copyIcon"
                ) { copied ->
                    Icon(
                        imageVector = if (copied) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.ContentCopy
                        },
                        contentDescription = null,
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun sourceLabel(source: ExtractionMode): String =
    stringResource(
        id = when (source) {
            ExtractionMode.ACCESSIBILITY -> R.string.mode_accessibility
            ExtractionMode.OCR -> R.string.mode_ocr
        }
    )

private fun formatTime(
    timestamp: Long
): String = SimpleDateFormat(
    "MMM d, HH:mm",
    Locale.getDefault()
).format(Date(timestamp))
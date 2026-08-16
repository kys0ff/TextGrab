package off.kys.textgrab.ui.screens.ocr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
import off.kys.textgrab.ui.theme.TextGrabTheme
import org.koin.androidx.compose.koinViewModel

class OcrPackageScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<OcrPackageViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        OcrPackageContent(
            state = state,
            onEvent = viewModel::onEvent,
            onPop = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrPackageContent(
    state: OcrPackageState,
    onEvent: (OcrPackageEvent) -> Unit,
    onPop: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val lazyListState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ocr_package_label_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(OcrPackageEvent.Refresh) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.ocr_package_action_button_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OcrIntroBanner()
                }

                items(
                    items = state.packages,
                    key = { it.displayName }
                ) { pkg ->
                    OcrPackageCardModern(
                        pkg = pkg,
                        onEvent = onEvent
                    )
                }
            }

            if (state.deleteConfirmation != null) {
                DeleteConfirmationDialog(
                    conf = state.deleteConfirmation,
                    onConfirm = { pkg, version ->
                        onEvent(
                            OcrPackageEvent.ConfirmDelete(
                                pkg,
                                version
                            )
                        )
                    },
                    onDismiss = { onEvent(OcrPackageEvent.DismissDeleteDialog) }
                )
            }
        }
    }
}

@Composable
private fun OcrIntroBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = stringResource(R.string.ocr_package_label_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun OcrPackageCardModern(
    pkg: OcrPackage,
    onEvent: (OcrPackageEvent) -> Unit
) {
    val anyDownloaded = pkg.versions.any { it.downloadState is DownloadState.Downloaded }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            PackageHeader(
                packageName = pkg.displayName,
                anyDownloaded = anyDownloaded,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pkg.versions.forEach { version ->
                    OcrVersionRowModern(
                        version = version,
                        onDownload = {
                            onEvent(OcrPackageEvent.Download(pkg, version.version, version.url))
                        },
                        onDelete = {
                            onEvent(OcrPackageEvent.Delete(pkg, version.version))
                        },
                        onSetDefault = {
                            onEvent(OcrPackageEvent.SetDefault(pkg, version.version))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageHeader(
    packageName: String,
    anyDownloaded: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = packageName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        AnimatedVisibility(
            visible = anyDownloaded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrVersionRowModern(
    version: OcrVersion,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val style = versionStyle(version.version)
    val state = version.downloadState
    val isDefault = version.isDefault
    val isDownloaded = state is DownloadState.Downloaded

    val backgroundColor by animateColorAsState(
        targetValue = if (isDefault) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(220),
        label = "versionBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = if (isDownloaded) onSetDefault else ({})
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionIconModern(style = style, selected = isDefault)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = versionName(version.version),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDefault) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (version.isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RecommendedPill()
                    }
                }
                Text(
                    text = stringResource(R.string.ocr_package_label_size_mb, version.sizeBytes / 1_000_000),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDefault) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            VersionActionsModern(
                state = state,
                isDefault = isDefault,
                accentColor = style.accent,
                onDownload = onDownload,
                onDelete = onDelete,
                onSetDefault = onSetDefault
            )
        }
    }
}

@Composable
private fun RecommendedPill() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.height(22.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.ocr_package_label_recommended),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun VersionIconModern(style: VersionStyle, selected: Boolean) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "iconContainer"
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = style.painter,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else style.accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun VersionActionsModern(
    state: DownloadState,
    isDefault: Boolean,
    accentColor: Color,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    when (state) {
        is DownloadState.Downloaded -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDefault) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    FilledTonalIconButton(onClick = onSetDefault, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        is DownloadState.Downloading -> {
            DownloadProgressCompact(progress = state.progress, accent = accentColor)
        }

        else -> {
            FilledIconButton(onClick = onDownload, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCompact(progress: Float, accent: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "dlProg"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Text(
            text = stringResource(R.string.ocr_package_label_percent, (progress * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationDialog(
    conf: DeleteConfirmation,
    onConfirm: (OcrPackage, TesseractVersion) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(conf.pkg, conf.version) }) {
                Text(
                    text = stringResource(R.string.ocr_package_action_button_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_action_button_cancel))
            }
        },
        title = { Text(stringResource(R.string.ocr_package_delete_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.ocr_package_delete_confirm_desc,
                    conf.pkg.displayName,
                    versionName(conf.version)
                )
            )
        },
        shape = RoundedCornerShape(28.dp)
    )
}

private data class VersionStyle(val painter: Painter, val accent: Color)

@Composable
private fun versionStyle(version: TesseractVersion): VersionStyle {
    return when (version) {
        TesseractVersion.FAST -> VersionStyle(
            painterResource(R.drawable.ic_bolt),
            MaterialTheme.colorScheme.secondary
        )

        TesseractVersion.STANDARD -> VersionStyle(
            painterResource(R.drawable.ic_scale),
            MaterialTheme.colorScheme.primary
        )

        TesseractVersion.BEST -> VersionStyle(
            painterResource(R.drawable.ic_diamond),
            MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun versionName(version: TesseractVersion): String = when (version) {
    TesseractVersion.FAST -> stringResource(R.string.ocr_package_label_version_fast)
    TesseractVersion.STANDARD -> stringResource(R.string.ocr_package_label_version_standard)
    TesseractVersion.BEST -> stringResource(R.string.ocr_package_label_version_best)
}

@Preview(showBackground = true)
@Composable
private fun OcrPackageScreenPreview() {
    TextGrabTheme {
        OcrPackageContent(
            state = OcrPackageState(
                packages = listOf(
                    OcrPackage(
                        language = OcrLanguage.LATIN,
                        displayName = "English",
                        tessCode = "eng",
                        versions = listOf(
                            OcrVersion(
                                TesseractVersion.FAST,
                                "url",
                                10_000_000L,
                                true,
                                DownloadState.Downloaded,
                                true
                            ),
                            OcrVersion(
                                TesseractVersion.STANDARD,
                                "url",
                                20_000_000L,
                                false,
                                DownloadState.Downloading(0.45f),
                                false
                            )
                        )
                    )
                )
            ),
            onEvent = {},
            onPop = {}
        )
    }
}
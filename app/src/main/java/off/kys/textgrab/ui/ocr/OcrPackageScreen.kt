package off.kys.textgrab.ui.ocr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion

class OcrPackageScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<OcrPackageViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            stringResource(R.string.ocr_download_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(OcrPackageEvent.Refresh) }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ocr_refresh))
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
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
                    Text(
                        stringResource(R.string.ocr_download_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                items(state.packages) { pkg ->
                    OcrPackageCard(
                        pkg = pkg,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

private data class VersionStyle(
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun versionStyle(version: TesseractVersion): VersionStyle = when (version) {
    TesseractVersion.FAST -> VersionStyle(Icons.Filled.Bolt, Color(0xFFFFA000))
    TesseractVersion.STANDARD -> VersionStyle(Icons.Filled.Scale, MaterialTheme.colorScheme.primary)
    TesseractVersion.BEST -> VersionStyle(Icons.Filled.Diamond, Color(0xFF8E24AA))
}

@Composable
fun OcrPackageCard(
    pkg: OcrPackage,
    onEvent: (OcrPackageEvent) -> Unit
) {
    val anyDownloaded = pkg.versions.any {
        it.downloadState is DownloadState.Downloaded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (anyDownloaded) 1.dp else 0.dp
        ),
        border = if (anyDownloaded)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        else null
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = pkg.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(visible = anyDownloaded, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            pkg.versions.forEachIndexed { index, version ->
                OcrVersionRow(
                    version = version,
                    onDownload = { onEvent(OcrPackageEvent.Download(pkg, version.version, version.url)) },
                    onDelete = { onEvent(OcrPackageEvent.Delete(pkg, version.version)) },
                    onSetDefault = { onEvent(OcrPackageEvent.SetDefault(pkg, version.version)) }
                )
                if (index < pkg.versions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
fun OcrVersionRow(
    version: OcrVersion,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val style = versionStyle(version.version)
    val state = version.downloadState
    val isDefault = version.isDefault
    val isDownloaded = state is DownloadState.Downloaded

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(style.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (version.version) {
                        TesseractVersion.FAST -> stringResource(R.string.ocr_version_fast)
                        TesseractVersion.STANDARD -> stringResource(R.string.ocr_version_standard)
                        TesseractVersion.BEST -> stringResource(R.string.ocr_version_best)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (version.isRecommended) {
                    Badge(
                        modifier = Modifier.padding(start = 8.dp),
                        containerColor = style.accent.copy(alpha = 0.18f),
                        contentColor = style.accent
                    ) {
                        Text(
                            stringResource(R.string.ocr_recommended),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = "${version.sizeBytes / 1_000_000} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = state is DownloadState.Downloading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val progress = (state as? DownloadState.Downloading)?.progress ?: 0f
                    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(8.dp),
                        color = style.accent,
                        trackColor = style.accent.copy(alpha = 0.15f)
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    IconButton(onClick = onSetDefault) {
                        Icon(
                            if (isDefault) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(R.string.ocr_set_default),
                            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "trailingState"
                ) { targetState ->
                    when (targetState) {
                        is DownloadState.Downloaded -> {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.ocr_delete),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                        is DownloadState.Downloading -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { targetState.progress },
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.5.dp,
                                    color = style.accent,
                                    trackColor = style.accent.copy(alpha = 0.15f)
                                )
                                Text(
                                    "${(targetState.progress * 100).toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            IconButton(onClick = onDownload) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(style.accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = stringResource(R.string.ocr_download),
                                        tint = style.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

package off.kys.textgrab.ui.ocr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrPackageScreen(
    viewModel: OcrPackageViewModel = viewModel(),
    onBack: () -> Unit
) {
    val packages by viewModel.packages.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.ocr_download_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.ocr_download_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(packages) { pkg ->
                OcrPackageCard(
                    pkg = pkg,
                    states = downloadStates,
                    onDownload = viewModel::download,
                    onDelete = viewModel::delete
                )
            }
        }
    }
}

/** Visual identity for each Tesseract quality tier. */
private data class VersionStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    states: Map<String, DownloadState>,
    onDownload: (OcrPackage, TesseractVersion, String) -> Unit,
    onDelete: (OcrPackage, TesseractVersion) -> Unit
) {
    val anyDownloaded = pkg.versions.any {
        states["${pkg.tessCode}_${it.version}"] is DownloadState.Downloaded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (anyDownloaded) 2.dp else 0.dp),
        border = if (anyDownloaded)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        else null
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = pkg.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
                val state = states["${pkg.tessCode}_${version.version}"] ?: DownloadState.NotDownloaded
                OcrVersionRow(
                    version = version,
                    state = state,
                    onDownload = { onDownload(pkg, version.version, version.url) },
                    onDelete = { onDelete(pkg, version.version) }
                )
                if (index < pkg.versions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun OcrVersionRow(
    version: OcrVersion,
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val style = versionStyle(version.version)

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(style.accent.copy(alpha = 0.15f)),
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
                    fontWeight = FontWeight.Bold
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
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .height(6.dp),
                        color = style.accent,
                        trackColor = style.accent.copy(alpha = 0.15f)
                    )
                }
            }
        },
        trailingContent = {
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
    )
}
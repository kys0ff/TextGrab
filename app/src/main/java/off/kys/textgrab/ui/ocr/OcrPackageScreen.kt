package off.kys.textgrab.ui.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
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
                title = { Text(stringResource(R.string.ocr_download_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
fun OcrPackageCard(
    pkg: OcrPackage,
    states: Map<String, DownloadState>,
    onDownload: (OcrPackage, TesseractVersion, String) -> Unit,
    onDelete: (OcrPackage, TesseractVersion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = pkg.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
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
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun OcrVersionRow(
    version: off.kys.textgrab.ocr.model.OcrVersion,
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    style = MaterialTheme.typography.bodySmall
                )
                if (state is DownloadState.Downloading) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        },
        trailingContent = {
            when (state) {
                is DownloadState.Downloaded -> {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.ocr_delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
                is DownloadState.Downloading -> {
                    Text(
                        stringResource(R.string.ocr_downloading, (state.progress * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.ocr_download))
                    }
                }
            }
        }
    )
}

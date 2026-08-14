package off.kys.textgrab.ui.screens.ocr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
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
            navigator = navigator
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrPackageContent(
    state: OcrPackageState,
    onEvent: (OcrPackageEvent) -> Unit,
    navigator: Navigator
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.ocr_download_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.pop() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onEvent(OcrPackageEvent.Refresh)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.ocr_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OcrIntro() }

            items(
                items = state.packages,
                key = { it.displayName }
            ) { pkg ->
                OcrPackageCard(
                    pkg = pkg,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun OcrIntro() {
    Column(
        modifier = Modifier.padding(
            horizontal = 4.dp,
            vertical = 4.dp
        )
    ) {
        Text(
            text = stringResource(R.string.ocr_download_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/**
 * FAST      -> secondary
 * STANDARD  -> primary
 * BEST      -> tertiary
 */
private data class VersionStyle(
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun versionStyle(
    version: TesseractVersion
): VersionStyle {
    val colors = MaterialTheme.colorScheme

    return when (version) {
        TesseractVersion.FAST -> {
            VersionStyle(
                icon = Icons.Default.Bolt,
                accent = colors.secondary
            )
        }

        TesseractVersion.STANDARD -> {
            VersionStyle(
                icon = Icons.Default.Scale,
                accent = colors.primary
            )
        }

        TesseractVersion.BEST -> {
            VersionStyle(
                icon = Icons.Default.Diamond,
                accent = colors.tertiary
            )
        }
    }
}

@Composable
private fun OcrPackageCard(
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            PackageHeader(
                packageName = pkg.displayName,
                anyDownloaded = anyDownloaded
            )

            Spacer(modifier = Modifier.height(4.dp))

            pkg.versions.forEach { version ->
                OcrVersionRow(
                    version = version,
                    onDownload = {
                        onEvent(
                            OcrPackageEvent.Download(
                                pkg,
                                version.version,
                                version.url
                            )
                        )
                    },
                    onDelete = {
                        onEvent(
                            OcrPackageEvent.Delete(
                                pkg,
                                version.version
                            )
                        )
                    },
                    onSetDefault = {
                        onEvent(
                            OcrPackageEvent.SetDefault(
                                pkg,
                                version.version
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PackageHeader(
    packageName: String,
    anyDownloaded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = packageName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.ocr_download_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = anyDownloaded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrVersionRow(
    version: OcrVersion,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val style = versionStyle(version.version)

    val state = version.downloadState
    val isDefault = version.isDefault
    val isDownloaded = state is DownloadState.Downloaded
    val isDownloading = state is DownloadState.Downloading

    val rowContainerColor by animateColorAsState(
        targetValue = when {
            isDefault ->
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)

            else ->
                MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(180),
        label = "versionRowContainer"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = rowContainerColor,
        tonalElevation = if (isDefault) 1.dp else 0.dp,
        onClick = if (isDownloaded) onSetDefault else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            VersionIcon(
                style = style,
                selected = isDefault
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = versionName(version.version),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (version.isRecommended) {
                        Spacer(modifier = Modifier.size(8.dp))

                        RecommendedLabel()
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${version.sizeBytes / 1_000_000} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isDefault) {
                        Spacer(modifier = Modifier.size(6.dp))

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.size(6.dp))

                        Text(
                            text = stringResource(R.string.ocr_set_default),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            when {
                isDownloaded -> {
                    DownloadedActions(
                        isDefault = isDefault,
                        onSetDefault = onSetDefault,
                        onDelete = onDelete
                    )
                }

                isDownloading -> {
                    DownloadProgressIndicator(
                        progress = state.progress,
                        accent = style.accent
                    )
                }

                else -> {
                    DownloadButton(onClick = onDownload)
                }
            }
        }
    }
}

@Composable
private fun VersionIcon(
    style: VersionStyle,
    selected: Boolean
) {
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }

    val iconTint =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            style.accent
        }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun RecommendedLabel() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = stringResource(R.string.ocr_recommended),
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 3.dp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DownloadedActions(
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        /*
         * Material 3 selected-state action.
         *
         * Instead of RadioButton/checkbox styling, the selected
         * model gets a filled primary action containing a check.
         */
        if (isDefault) {
            FilledIconButton(
                onClick = {},
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(
                        R.string.ocr_set_default
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            FilledTonalIconButton(
                onClick = onSetDefault,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(
                        R.string.ocr_set_default
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.size(4.dp))

        /*
         * Error-tonal delete action instead of a raw red icon.
         *
         * This feels much closer to Material 3 destructive actions.
         */
        FilledTonalIconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(
                    R.string.ocr_delete
                ),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DownloadProgressIndicator(
    progress: Float,
    accent: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180),
        label = "downloadProgress"
    )

    val percentage =
        (progress.coerceIn(0f, 1f) * 100).toInt()

    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(38.dp),
            strokeWidth = 3.dp,
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        Text(
            text = "$percentage",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DownloadButton(onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = stringResource(
                R.string.ocr_download
            ),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun versionName(
    version: TesseractVersion
): String {
    return when (version) {
        TesseractVersion.FAST ->
            stringResource(R.string.ocr_version_fast)

        TesseractVersion.STANDARD ->
            stringResource(R.string.ocr_version_standard)

        TesseractVersion.BEST ->
            stringResource(R.string.ocr_version_best)
    }
}
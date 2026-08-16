package off.kys.textgrab.ui.screens.ocr.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun OcrVersionRow(
    version: OcrVersion,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = versionStyle(version.version)
    val state = version.downloadState
    val isDefault = version.isDefault
    val isDownloaded = state is DownloadState.Downloaded

    val backgroundColor by animateColorAsState(
        targetValue = if (isDefault) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(220),
        label = "versionBg",
    )

    Surface(
        modifier = modifier
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
            VersionIcon(style = style, selected = isDefault)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = versionName(version.version),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
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

            VersionActions(
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
private fun VersionIcon(style: VersionStyle, selected: Boolean) {
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
private fun VersionActions(
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

@Preview(showBackground = true)
@Composable
private fun OcrVersionRowDownloadedPreview() {
    TextGrabTheme {
        OcrVersionRow(
            version = OcrVersion(
                version = TesseractVersion.BEST,
                url = "url",
                sizeBytes = 15_000_000L,
                isRecommended = true,
                downloadState = DownloadState.Downloaded,
                isDefault = true
            ),
            onDownload = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OcrVersionRowDownloadingPreview() {
    TextGrabTheme {
        OcrVersionRow(
            version = OcrVersion(
                version = TesseractVersion.STANDARD,
                url = "url",
                sizeBytes = 15_000_000L,
                isRecommended = false,
                downloadState = DownloadState.Downloading(0.5f),
                isDefault = false
            ),
            onDownload = {},
            onDelete = {},
            onSetDefault = {}
        )
    }
}

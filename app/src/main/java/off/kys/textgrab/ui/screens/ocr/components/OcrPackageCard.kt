package off.kys.textgrab.ui.screens.ocr.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.DownloadState
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.OcrVersion
import off.kys.textgrab.ocr.model.TesseractVersion
import off.kys.textgrab.ui.screens.ocr.OcrPackageEvent
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun OcrPackageCard(
    pkg: OcrPackage,
    onEvent: (OcrPackageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyDownloaded = pkg.versions.any { it.downloadState is DownloadState.Downloaded }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    OcrVersionRow(
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

@Preview(showBackground = true)
@Composable
private fun OcrPackageCardPreview() {
    TextGrabTheme {
        OcrPackageCard(
            pkg = OcrPackage(
                language = OcrLanguage.LATIN,
                displayName = "English",
                tessCode = "eng",
                versions = listOf(
                    OcrVersion(
                        version = TesseractVersion.FAST,
                        url = "url",
                        sizeBytes = 10_000_000L,
                        isRecommended = true,
                        downloadState = DownloadState.Downloaded,
                        isDefault = true
                    ),
                    OcrVersion(
                        version = TesseractVersion.BEST,
                        url = "url",
                        sizeBytes = 15_000_000L,
                        isRecommended = false,
                        downloadState = DownloadState.NotDownloaded,
                        isDefault = false
                    )
                )
            ),
            onEvent = {}
        )
    }
}

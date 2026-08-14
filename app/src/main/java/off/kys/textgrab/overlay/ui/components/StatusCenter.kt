package off.kys.textgrab.overlay.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.OverlayStatus

@Composable
fun StatusCenter(
    status: OverlayStatus,
    mode: ExtractionMode,
    modifier: Modifier = Modifier,
    onSwitchToOcr: () -> Unit,
    onRescan: () -> Unit,
    onOpenDownload: () -> Unit,
) {
    when (status) {
        OverlayStatus.LoadingModel -> InfoCard(modifier) {
            CircularProgressIndicator(
                Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.overlay_label_loading_model),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        OverlayStatus.Scanning -> InfoCard(modifier) {
            CircularProgressIndicator(
                Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.overlay_label_scanning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        OverlayStatus.Empty -> InfoCard(modifier) {
            IllustrationBadge(icon = Icons.Filled.SearchOff)
            Spacer(Modifier.height(16.dp))
            val message = if (mode == ExtractionMode.ACCESSIBILITY) {
                stringResource(R.string.overlay_label_empty_accessibility)
            } else {
                stringResource(R.string.overlay_label_empty_ocr)
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (mode == ExtractionMode.ACCESSIBILITY) {
                FilledTonalButton(
                    onClick = onSwitchToOcr,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.common_mode_label_ocr))
                }
            } else {
                FilledTonalButton(
                    onClick = onRescan,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.overlay_action_button_rescan))
                }
            }
        }

        is OverlayStatus.Error -> InfoCard(
            modifier,
            accentColor = MaterialTheme.colorScheme.error
        ) {
            IllustrationBadge(icon = Icons.Filled.ErrorOutline, error = true)
            Spacer(Modifier.height(16.dp))
            Text(
                status.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            if (mode == ExtractionMode.OCR) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onOpenDownload,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ocr_package_label_title))
                }
            }
        }

        OverlayStatus.Idle, is OverlayStatus.Ready -> Unit
    }
}

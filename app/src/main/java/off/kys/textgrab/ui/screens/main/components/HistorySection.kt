package off.kys.textgrab.ui.screens.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import off.kys.textgrab.R
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import off.kys.textgrab.ui.theme.TextGrabTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HistorySection(
    history: List<HistoryEntry>,
    onClear: () -> Unit,
    onCopy: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val visible = history.take(10)
                    visible.forEachIndexed { index, entry ->
                        HistoryItem(entry = entry) { onCopy(entry) }
                        if (index != visible.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(entry: HistoryEntry, onCopy: () -> Unit) {
    var justCopied by remember { mutableStateOf(value = false) }
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
                modifier = Modifier.size(40.dp)
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
        shape = RoundedCornerShape(20.dp),
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
private fun HistorySectionPreview() {
    TextGrabTheme {
        HistorySection(
            history = listOf(
                HistoryEntry(1L, "Sample extracted text 1", System.currentTimeMillis(), ExtractionMode.ACCESSIBILITY),
                HistoryEntry(2L, "Sample extracted text 2", System.currentTimeMillis() - 1000000, ExtractionMode.OCR)
            ),
            onClear = {},
            onCopy = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistorySectionPreview() {
    TextGrabTheme {
        HistorySection(
            history = emptyList(),
            onClear = {},
            onCopy = {}
        )
    }
}

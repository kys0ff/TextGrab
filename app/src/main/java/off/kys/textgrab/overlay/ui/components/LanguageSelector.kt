package off.kys.textgrab.overlay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.TessDataStore

@Composable
fun LanguageSelector(
    selected: OcrLanguage,
    onSelect: (OcrLanguage) -> Unit,
    onOpenDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(OcrLanguage.getSortedEntries(context)) { lang ->
            val isInstalled = remember(lang) {
                when (lang) {
                    OcrLanguage.AUTO -> OcrLanguage.entries.any {
                        it != OcrLanguage.AUTO && TessDataStore.hasAnyInstalled(
                            context,
                            it.toTessCode()
                        )
                    }

                    else -> TessDataStore.hasAnyInstalled(context, lang.toTessCode())
                }
            }

            FilterChip(
                selected = selected == lang,
                onClick = { onSelect(lang) },
                label = {
                    Text(
                        stringResource(lang.toDisplayString()),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = if (!isInstalled) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_error_outline),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error
                )
            )
        }

        item {
            FilledTonalIconButton(
                onClick = onOpenDownload,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_language),
                    contentDescription = stringResource(R.string.ocr_package_label_title),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun OcrLanguage.toDisplayString(): Int = when (this) {
    OcrLanguage.LATIN -> R.string.ocr_lang_label_latin
    OcrLanguage.ARABIC -> R.string.ocr_lang_label_arabic
    OcrLanguage.FRENCH -> R.string.ocr_lang_label_french
    OcrLanguage.GERMAN -> R.string.ocr_lang_label_german
    OcrLanguage.CHINESE -> R.string.ocr_lang_label_chinese
    OcrLanguage.JAPANESE -> R.string.ocr_lang_label_japanese
    OcrLanguage.KOREAN -> R.string.ocr_lang_label_korean
    OcrLanguage.AUTO -> R.string.ocr_lang_label_auto
}

fun OcrLanguage.toTessCode(): String = when (this) {
    OcrLanguage.LATIN -> "eng"
    OcrLanguage.ARABIC -> "ara"
    OcrLanguage.FRENCH -> "fra"
    OcrLanguage.GERMAN -> "deu"
    OcrLanguage.CHINESE -> "chi_sim"
    OcrLanguage.JAPANESE -> "jpn"
    OcrLanguage.KOREAN -> "kor"
    OcrLanguage.AUTO -> "auto"
}

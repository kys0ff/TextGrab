package off.kys.textgrab.ui.screens.ocr.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.core.model.OcrLanguage
import off.kys.textgrab.ocr.model.OcrPackage
import off.kys.textgrab.ocr.model.TesseractVersion
import off.kys.textgrab.ui.screens.ocr.DeleteConfirmation
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun DeleteConfirmationDialog(
    conf: DeleteConfirmation,
    onConfirm: (OcrPackage, TesseractVersion) -> Unit,
    onDismiss: () -> Unit,
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

@Composable
fun versionName(version: TesseractVersion): String = when (version) {
    TesseractVersion.FAST -> stringResource(R.string.ocr_package_label_version_fast)
    TesseractVersion.STANDARD -> stringResource(R.string.ocr_package_label_version_standard)
    TesseractVersion.BEST -> stringResource(R.string.ocr_package_label_version_best)
}

@Preview
@Composable
private fun DeleteConfirmationDialogPreview() {
    TextGrabTheme {
        DeleteConfirmationDialog(
            conf = DeleteConfirmation(
                pkg = OcrPackage(OcrLanguage.LATIN, "English", "eng", emptyList()),
                version = TesseractVersion.STANDARD
            ),
            onConfirm = { _, _ -> },
            onDismiss = {}
        )
    }
}

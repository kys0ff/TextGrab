package off.kys.textgrab.ui.screens.main.components

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
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
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
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.history_action_button_clear),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_action_button_cancel))
            }
        },
        title = { Text(stringResource(R.string.history_clear_confirm_title)) },
        text = { Text(stringResource(R.string.history_clear_confirm_desc)) },
        shape = RoundedCornerShape(28.dp)
    )
}

@Preview
@Composable
private fun ClearHistoryDialogPreview() {
    TextGrabTheme {
        ClearHistoryDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

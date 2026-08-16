package off.kys.textgrab.ui.screens.main.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    TextGrabTheme {
        SectionHeader(title = "Permissions")
    }
}

package off.kys.textgrab.ui.screens.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R
import off.kys.textgrab.ui.screens.main.PermissionUiState
import off.kys.textgrab.ui.theme.TextGrabTheme

@Composable
fun PermissionGrid(
    permissions: PermissionUiState,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOcr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PermissionItem(
            icon = painterResource(R.drawable.ic_accessibility),
            title = stringResource(R.string.permission_accessibility_label_title),
            granted = permissions.accessibility,
            onAction = onOpenAccessibility
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_layers),
            title = stringResource(R.string.permission_overlay_label_title),
            granted = permissions.overlay,
            onAction = onOpenOverlay
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_notifications),
            title = stringResource(R.string.permission_notifications_label_title),
            granted = permissions.notifications,
            onAction = onOpenNotifications
        )
        PermissionItem(
            icon = painterResource(R.drawable.ic_image),
            title = stringResource(R.string.permission_projection_label_title),
            granted = true,
            onAction = onOcr,
            isInfo = true
        )
    }
}

@Composable
private fun PermissionItem(
    icon: Painter,
    title: String,
    granted: Boolean,
    onAction: () -> Unit,
    isInfo: Boolean = false
) {
    val containerColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "permContainer"
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "permIconContainer"
    )

    Surface(
        onClick = onAction,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isInfo -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                granted -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                else -> {
                    GrantPill()
                }
            }
        }
    }
}

@Composable
private fun GrantPill() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = stringResource(R.string.common_action_button_grant),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionGridPreview() {
    TextGrabTheme {
        PermissionGrid(
            permissions = PermissionUiState(accessibility = true, overlay = false, notifications = true),
            onOpenAccessibility = {},
            onOpenOverlay = {},
            onOpenNotifications = {},
            onOcr = {}
        )
    }
}

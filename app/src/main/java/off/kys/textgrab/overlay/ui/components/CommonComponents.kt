package off.kys.textgrab.overlay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IllustrationBadge(
    icon: ImageVector,
    error: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (error) scheme.errorContainer else scheme.secondaryContainer
    val content = if (error) scheme.onErrorContainer else scheme.onSecondaryContainer
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                1.dp,
                accentColor?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.6f
                ),
            ),
        ) {
            Column(
                Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content() }
        }
    }
}

package off.kys.textgrab.overlay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import off.kys.textgrab.core.model.GrabbedText

@Composable
fun TextBox(
    element: GrabbedText,
    selected: Boolean,
    multiSelect: Boolean,
    widthDp: Dp,
    heightDp: Dp,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    val borderColor = if (selected) scheme.primary else scheme.primary.copy(alpha = 0.55f)
    val fillColor = if (selected) {
        scheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        scheme.surfaceContainerHighest.copy(alpha = 0.90f)
    }
    val textColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface

    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        label = "textBoxBorderWidth",
    )

    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "textBoxSelectionScale",
    )

    val appear = remember(element.id) { Animatable(0f) }
    LaunchedEffect(element.id) {
        appear.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        Modifier
            .offset { IntOffset(element.left, element.top) }
            .size(width = widthDp.coerceAtLeast(28.dp), height = heightDp.coerceAtLeast(22.dp))
            .graphicsLayer {
                val scale = appear.value * selectionScale
                scaleX = scale
                scaleY = scale
                alpha = appear.value
            }
            .shadow(
                elevation = if (selected) 4.dp else 1.dp,
                shape = RoundedCornerShape(8.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(8.dp))
            .background(fillColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(element.id) {
                detectTapGestures(onTap = { onSelect() })
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (element.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            Text(
                text = element.text,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = textColor,
            )
        }

        if (multiSelect) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else scheme.surface.copy(alpha = 0.8f))
                    .border(
                        width = 1.dp,
                        color = if (selected) scheme.primary else scheme.outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

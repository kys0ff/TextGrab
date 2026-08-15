package off.kys.textgrab.overlay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R

@Composable
fun InlineAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            modifier = modifier
        )
    },
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures { onDismissRequest() }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Surface(
                modifier = modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp, max = 320.dp)
                    .semantics { dialog() }
                    .pointerInput(Unit) {
                        detectTapGestures { /* Consume inner taps */ }
                    },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = if (icon != null) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    if (icon != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.secondary
                        ) {
                            Box(Modifier.padding(bottom = 16.dp)) {
                                icon()
                            }
                        }
                    }

                    if (title != null) {
                        Box(Modifier.padding(bottom = 16.dp)) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurface
                            ) {
                                ProvideTextStyle(
                                    MaterialTheme.typography.headlineSmall.copy(
                                        textAlign = if (icon != null) TextAlign.Center else TextAlign.Start
                                    )
                                ) {
                                    title()
                                }
                            }
                        }
                    }

                    if (text != null) {
                        Box(Modifier.padding(bottom = 24.dp)) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                ProvideTextStyle(
                                    MaterialTheme.typography.bodyMedium.copy(
                                        textAlign = if (icon != null) TextAlign.Center else TextAlign.Start
                                    )
                                ) {
                                    text()
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                            if (dismissButton != null) {
                                dismissButton()
                                Spacer(Modifier.width(8.dp))
                            }
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
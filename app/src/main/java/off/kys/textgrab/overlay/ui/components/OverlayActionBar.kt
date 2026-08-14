package off.kys.textgrab.overlay.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import off.kys.textgrab.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayActionBar(
    multiSelect: Boolean,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onToggleSelect: () -> Unit,
    onCopySelected: () -> Unit,
    onCopyAll: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.6f)
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BadgedBox(
                badge = {
                    if (multiSelect && selectedCount > 0) {
                        Badge(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ) {
                            AnimatedContent(
                                targetState = selectedCount,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { it } + fadeIn()) togetherWith
                                                (slideOutVertically { -it } + fadeOut())
                                    } else {
                                        (slideInVertically { -it } + fadeIn()) togetherWith
                                                (slideOutVertically { it } + fadeOut())
                                    }
                                },
                                label = "selectedCountBadge",
                            ) { count -> Text("$count") }
                        }
                    }
                },
            ) {
                FilterChip(
                    selected = multiSelect,
                    onClick = onToggleSelect,
                    label = { Text(stringResource(R.string.overlay_action_button_select)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = scheme.secondaryContainer,
                        selectedLabelColor = scheme.onSecondaryContainer,
                        selectedLeadingIconColor = scheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = multiSelect,
                        borderColor = scheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                    ),
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = multiSelect,
                transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                label = "copyButton",
            ) { isMultiSelect ->
                if (isMultiSelect) {
                    Button(
                        onClick = onCopySelected,
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.overlay_action_button_copy_selected, selectedCount))
                    }
                } else {
                    Button(
                        onClick = onCopyAll,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            Icons.Filled.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.overlay_action_button_copy_all))
                    }
                }
            }
        }
    }
}

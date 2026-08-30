package com.m57.hermescontrol.ui.chat.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m57.hermescontrol.R

/**
 * Bottom toolbar row for the chat composer — v2 (rtl-design-upgrade).
 *
 * Layout:
 *  [📎 attach] [model chip] [←spacer→] [🧠 reasoning] [🎙 mic]
 *
 * Material icons replace the previous emoji glyphs and all icons sit inside
 * glass bubbles for visual consistency with the new top bar. The reasoning
 * chip opens a dropdown menu to pick a level (instead of cycling). When
 * [canDisableReasoning] is false the "None" level is disabled with a
 * "reasoning always on" hint (issue #946). When [supportsReasoning] is false
 * the model takes no reasoning parameter and the chip is disabled.
 */
@Composable
fun ComposerToolbar(
    isConnected: Boolean,
    currentSessionModel: String?,
    reasoningLevel: String?,
    isListening: Boolean,
    onAttachTap: () -> Unit,
    onModelTap: () -> Unit,
    onReasoningSelected: (String?) -> Unit,
    onMicTap: () -> Unit,
    modifier: Modifier = Modifier,
    canDisableReasoning: Boolean? = null,
    supportsReasoning: Boolean? = null,
) {
    var showReasoningMenu by remember { mutableStateOf(false) }
    val reasoningDisabledForModel = supportsReasoning == false
    val canDisable = canDisableReasoning

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("composer_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Attach button — glass bubble, Material icon.
        IconButton(
            onClick = onAttachTap,
            enabled = isConnected,
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("attach_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.AttachFile,
                contentDescription = stringResource(R.string.chat_attach_file),
                modifier = Modifier.size(20.dp),
            )
        }

        // Model chip — takes available space, fixed height.
        FilterChip(
            selected = currentSessionModel != null,
            onClick = onModelTap,
            label = {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentSessionModel ?: "Model",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier =
                Modifier
                    .height(36.dp)
                    .testTag("model_chip"),
        )

        // Reasoning chip with dropdown menu (right side, next to mic).
        Box {
            FilterChip(
                selected = reasoningLevel != null,
                onClick = { showReasoningMenu = true },
                enabled = !reasoningDisabledForModel,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint =
                                if (reasoningDisabledForModel) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        )
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.size(4.dp),
                        )
                        Text(
                            text =
                                if (reasoningDisabledForModel) {
                                    "No reasoning"
                                } else {
                                    buildReasoningLabel(reasoningLevel)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier =
                    Modifier
                        .height(36.dp)
                        .testTag("reasoning_chip"),
            )

            DropdownMenu(
                expanded = showReasoningMenu,
                onDismissRequest = { showReasoningMenu = false },
            ) {
                Text(
                    text = "Reasoning",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                if (canDisable == false) {
                    Text(
                        text = "reasoning always on",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
                if (reasoningDisabledForModel) {
                    Text(
                        text = "no reasoning parameter for this model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
                HorizontalDivider()
                val allLevels =
                    listOf(
                        "none" to "None",
                        "minimal" to "Minimal",
                        "low" to "Low",
                        "medium" to "Med",
                        "high" to "High",
                        "xhigh" to "XHigh",
                        "max" to "Max",
                        "ultra" to "Ultra",
                    )
                allLevels.forEach { (level, label) ->
                    val isNone = level == "none"
                    val noneDisabled = isNone && (canDisable == false || reasoningDisabledForModel)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight =
                                    if (reasoningLevel == level) {
                                        FontWeight.SemiBold
                                    } else {
                                        null
                                    },
                                color =
                                    when {
                                        noneDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        reasoningLevel == level -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        },
                        onClick = {
                            showReasoningMenu = false
                            onReasoningSelected(level)
                        },
                        enabled = !noneDisabled && !reasoningDisabledForModel,
                    )
                }
            }
        }

        // Mic / Stop button — glass bubble, prominent when listening.
        IconButton(
            onClick = onMicTap,
            enabled = isConnected,
            colors =
                if (isListening) {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag(if (isListening) "mic_stop_button" else "mic_button"),
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription =
                    if (isListening) {
                        "Stop listening"
                    } else {
                        stringResource(R.string.chat_mic_desc)
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Build a human-readable label from a reasoning effort level.
 *
 * @param level One of: "none", "minimal", "low", "medium", "high",
 *              "xhigh", "max", "ultra", or null for model default.
 * @return Display string such as "None", "Low", "XHigh", "Ultra", etc.
 */
private fun buildReasoningLabel(level: String?): String =
    when (level) {
        null -> "Med"
        "none" -> "None"
        "minimal" -> "Minimal"
        "low" -> "Low"
        "medium" -> "Med"
        "high" -> "High"
        "xhigh" -> "XHigh"
        "max" -> "Max"
        "ultra" -> "Ultra"
        else -> level
    }
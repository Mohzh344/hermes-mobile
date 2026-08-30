package com.m57.hermescontrol.ui.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.m57.hermescontrol.R
import com.m57.hermescontrol.data.model.Attachment
import com.m57.hermescontrol.data.model.ProfileInfo
import com.m57.hermescontrol.data.ws.CommandBlocklist
import com.m57.hermescontrol.data.ws.CommandCatalog
import com.m57.hermescontrol.ui.chat.ChatInputPolicy
import com.m57.hermescontrol.ui.common.BotAvatar
import com.m57.hermescontrol.ui.common.GlassSurface
import com.m57.hermescontrol.ui.common.LocalGlassBlurEnabled

/**
 * The chat input bar — v2 (rtl-design-upgrade).
 *
 * Key changes vs the legacy composer:
 *  - Lifted off the system bottom inset: the composer floats ~14 dp above
 *    the navigation bar (instead of being glued to the edge). The `imePadding`
 *    modifier still keeps it above the soft keyboard.
 *  - Frosted-glass surface: a translucent tint + hairline border replaces
 *    the solid `Surface` with `tonalElevation`. Toggleable via the new
 *    "Glass blur" setting.
 *  - Material icons replace emoji glyphs for the attach dropdown items.
 *  - Spring-animated send-button pop instead of fade-only.
 *  - Suggested bubbles slide in/out smoothly above the composer when
 *    present.
 */
@Composable
fun ChatInputBar(
    inputFieldValue: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onMicTap: () -> Unit,
    isListening: Boolean,
    isAgentTyping: Boolean,
    isConnected: Boolean,
    commandCatalog: CommandCatalog,
    slashUsageCounts: Map<String, Int> = emptyMap(),
    pendingAttachments: List<Attachment> = emptyList(),
    availableBots: List<ProfileInfo> = emptyList(),
    onCameraTap: () -> Unit = {},
    onImageTap: () -> Unit = {},
    onFileTap: () -> Unit = {},
    onRemoveAttachment: (Int) -> Unit = {},
    onPreviewAttachment: (Attachment) -> Unit = {},
    // NEW: composer toolbar wiring
    currentSessionModel: String? = null,
    reasoningLevel: String? = null,
    onModelTap: () -> Unit = {},
    onReasoningTap: (String?) -> Unit = {},
    canDisableReasoning: Boolean? = null,
    supportsReasoning: Boolean? = null,
) {
    val canSend = ChatInputPolicy.canSend(inputFieldValue.text, pendingAttachments, isConnected)

    // Attachment menu state
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // When the keyboard opens, the composer slides up smoothly via imePadding;
    // we add a small extra lift above the system nav bar so the bar never
    // looks "glued" to the edge even on devices with no soft keys.
    val composerBottomPadding by animateDpAsState(
        targetValue = 14.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "composerBottomPadding",
    )

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        val glassEnabled = LocalGlassBlurEnabled.current
        val containerColor = MaterialTheme.colorScheme.surface
        val animatedTint by animateColorAsState(
            targetValue = containerColor,
            animationSpec = tween(durationMillis = 200),
            label = "composerTint",
        )
        val composerShape = RoundedCornerShape(26.dp)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = composerBottomPadding)
                    .padding(horizontal = 10.dp),
        ) {
            // Suggested bubbles — slash commands and @mentions. Rendered
            // ABOVE the composer surface as their own floating glass sheets
            // so they read as floating autocomplete UI.
            SlashCommandSuggestions(
                inputFieldValue = inputFieldValue,
                commandNames = commandCatalog.pairs.map { it[0] }.filter {
                    it.lowercase() !in CommandBlocklist.UNSUPPORTED
                },
                slashUsageCounts = slashUsageCounts,
                onPick = { cmd ->
                    onInputChange(ChatInputPolicy.commandFieldValue(cmd))
                },
            )
            MentionSuggestions(
                inputFieldValue = inputFieldValue,
                availableBots = availableBots,
                onPick = { bot ->
                    onInputChange(ChatInputPolicy.applyMention(inputFieldValue, bot.name))
                },
            )

            // The composer body itself.
            val composerModifier =
                Modifier
                    .fillMaxWidth()
                    .clip(composerShape)
                    .let { base ->
                        if (glassEnabled) {
                            base
                        } else {
                            base.background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                animatedTint.copy(alpha = 0.96f),
                                                animatedTint.copy(alpha = 0.92f),
                                            ),
                                    ),
                            )
                        }
                    }
            Box(modifier = composerModifier.testTag("chat_composer_root")) {
                if (glassEnabled) {
                    GlassSurface(cornerRadius = 26.dp, tint = animatedTint) {
                        ComposerBody(
                            inputFieldValue = inputFieldValue,
                            onInputChange = onInputChange,
                            isFocused = isFocused,
                            onFocusChange = { isFocused = it },
                            placeholderText = placeholderText(inputFieldValue.text, isConnected, isAgentTyping),
                            canSend = canSend,
                            onSend = onSend,
                            pendingAttachments = pendingAttachments,
                            onRemoveAttachment = onRemoveAttachment,
                            onPreviewAttachment = onPreviewAttachment,
                            isConnected = isConnected,
                            currentSessionModel = currentSessionModel,
                            reasoningLevel = reasoningLevel,
                            isListening = isListening,
                            onAttachTap = { showAttachmentMenu = true },
                            onModelTap = onModelTap,
                            onReasoningSelected = onReasoningTap,
                            onMicTap = onMicTap,
                            canDisableReasoning = canDisableReasoning,
                            supportsReasoning = supportsReasoning,
                            showAttachmentMenu = showAttachmentMenu,
                            onAttachmentMenuDismiss = { showAttachmentMenu = false },
                            onCameraTap = {
                                showAttachmentMenu = false
                                onCameraTap()
                            },
                            onImageTap = {
                                showAttachmentMenu = false
                                onImageTap()
                            },
                            onFileTap = {
                                showAttachmentMenu = false
                                onFileTap()
                            },
                        )
                    }
                } else {
                    CompositionLocalProvider(LocalGlassBlurEnabled provides false) {
                        ComposerBody(
                            inputFieldValue = inputFieldValue,
                            onInputChange = onInputChange,
                            isFocused = isFocused,
                            onFocusChange = { isFocused = it },
                            placeholderText = placeholderText(inputFieldValue.text, isConnected, isAgentTyping),
                            canSend = canSend,
                            onSend = onSend,
                            pendingAttachments = pendingAttachments,
                            onRemoveAttachment = onRemoveAttachment,
                            onPreviewAttachment = onPreviewAttachment,
                            isConnected = isConnected,
                            currentSessionModel = currentSessionModel,
                            reasoningLevel = reasoningLevel,
                            isListening = isListening,
                            onAttachTap = { showAttachmentMenu = true },
                            onModelTap = onModelTap,
                            onReasoningSelected = onReasoningTap,
                            onMicTap = onMicTap,
                            canDisableReasoning = canDisableReasoning,
                            supportsReasoning = supportsReasoning,
                            showAttachmentMenu = showAttachmentMenu,
                            onAttachmentMenuDismiss = { showAttachmentMenu = false },
                            onCameraTap = {
                                showAttachmentMenu = false
                                onCameraTap()
                            },
                            onImageTap = {
                                showAttachmentMenu = false
                                onImageTap()
                            },
                            onFileTap = {
                                showAttachmentMenu = false
                                onFileTap()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Resolve the placeholder string for the empty input state. */
@Composable
private fun placeholderText(
    text: String,
    isConnected: Boolean,
    isAgentTyping: Boolean,
): String =
    when {
        !isConnected -> stringResource(R.string.chat_input_placeholder_not_connected)
        ChatInputPolicy.showQueuePlaceholder(text, isAgentTyping) ->
            stringResource(R.string.chat_input_placeholder_queue)
        isAgentTyping -> stringResource(R.string.chat_input_placeholder_waiting)
        else -> stringResource(R.string.chat_input_placeholder_type_message)
    }

/**
 * The composer body — separated out so we can render it inside either a
 * glass surface (glass mode) or a plain Surface (fallback mode).
 */
@Composable
private fun ComposerBody(
    inputFieldValue: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    placeholderText: String,
    canSend: Boolean,
    onSend: () -> Unit,
    pendingAttachments: List<Attachment>,
    onRemoveAttachment: (Int) -> Unit,
    onPreviewAttachment: (Attachment) -> Unit,
    isConnected: Boolean,
    currentSessionModel: String?,
    reasoningLevel: String?,
    isListening: Boolean,
    onAttachTap: () -> Unit,
    onModelTap: () -> Unit,
    onReasoningSelected: (String?) -> Unit,
    onMicTap: () -> Unit,
    canDisableReasoning: Boolean?,
    supportsReasoning: Boolean?,
    showAttachmentMenu: Boolean,
    onAttachmentMenuDismiss: () -> Unit,
    onCameraTap: () -> Unit,
    onImageTap: () -> Unit,
    onFileTap: () -> Unit,
) {
    Column {
        // Attachment preview chips
        AnimatedVisibility(
            visible = pendingAttachments.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(pendingAttachments) { index, attachment ->
                    AttachmentChip(
                        attachment = attachment,
                        onPreview = { onPreviewAttachment(attachment) },
                        onRemove = { onRemoveAttachment(index) },
                    )
                }
            }
        }

        // TOP ROW: Input field with embedded send button
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = inputFieldValue,
                onValueChange = onInputChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp, max = 120.dp)
                        .padding(vertical = 4.dp)
                        .onFocusChanged { onFocusChange(it.isFocused) }
                        .testTag("chat_input"),
                enabled = isConnected,
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                singleLine = false,
                maxLines = 4,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border =
                            androidx.compose.foundation.BorderStroke(
                                width = if (isFocused) 1.5.dp else 0.5.dp,
                                color =
                                    if (isFocused) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    },
                            ),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .padding(start = 14.dp, end = 4.dp, top = 9.dp, bottom = 9.dp)
                                    .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (inputFieldValue.text.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f,
                                            ),
                                    )
                                }
                                innerTextField()
                            }

                            // Spring-animated send button (pop in / shrink out).
                            AnimatedContent(
                                targetState = canSend,
                                transitionSpec = {
                                    (scaleIn(initialScale = 0.4f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn())
                                        .togetherWith(scaleOut(targetScale = 0.4f, animationSpec = tween(120)) + fadeOut())
                                },
                                label = "send_toggle",
                            ) { showSend ->
                                if (showSend) {
                                    IconButton(
                                        onClick = onSend,
                                        enabled = canSend,
                                        colors =
                                            IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        modifier =
                                            Modifier
                                                .size(38.dp)
                                                .testTag("send_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription =
                                                stringResource(R.string.chat_send_desc),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        // BOTTOM ROW: Toolbar
        ComposerToolbar(
            isConnected = isConnected,
            currentSessionModel = currentSessionModel,
            reasoningLevel = reasoningLevel,
            isListening = isListening,
            onAttachTap = onAttachTap,
            onModelTap = onModelTap,
            onReasoningSelected = onReasoningSelected,
            onMicTap = onMicTap,
            modifier = Modifier.testTag("chat_composer_toolbar"),
            canDisableReasoning = canDisableReasoning,
            supportsReasoning = supportsReasoning,
        )

        // Attachment dropdown — Material icons instead of emoji.
        DropdownMenu(
            expanded = showAttachmentMenu,
            onDismissRequest = onAttachmentMenuDismiss,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_attach_camera)) },
                onClick = onCameraTap,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_attach_image)) },
                onClick = onImageTap,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_attach_file)) },
                onClick = onFileTap,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    }
}

/**
 * Slash-command autocomplete suggestions — rendered as a floating glass pill
 * ABOVE the composer surface so it reads as suggested completions.
 */
@Composable
private fun SlashCommandSuggestions(
    inputFieldValue: TextFieldValue,
    commandNames: List<String>,
    slashUsageCounts: Map<String, Int>,
    onPick: (String) -> Unit,
) {
    AnimatedVisibility(
        visible =
            inputFieldValue.text.startsWith("/") && !inputFieldValue.text.contains(" "),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val filteredCommands =
            ChatInputPolicy.sortSlashSuggestions(
                commandNames.filter {
                    it.startsWith(inputFieldValue.text, ignoreCase = true)
                },
                slashUsageCounts,
            )
        if (filteredCommands.isNotEmpty()) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    ),
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(filteredCommands, key = { it }) { cmd ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    cmd,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = { onPick(cmd) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * @-mention autocomplete suggestions — rendered as a floating glass pill.
 */
@Composable
private fun MentionSuggestions(
    inputFieldValue: TextFieldValue,
    availableBots: List<ProfileInfo>,
    onPick: (ProfileInfo) -> Unit,
) {
    val mentionQuery =
        remember(inputFieldValue.text, inputFieldValue.selection.end) {
            ChatInputPolicy.extractMentionQuery(
                inputFieldValue.text,
                inputFieldValue.selection.end,
            )
        }
    AnimatedVisibility(
        visible = mentionQuery != null && availableBots.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val filteredBots =
            remember(mentionQuery, availableBots) {
                if (mentionQuery == null) {
                    emptyList()
                } else {
                    availableBots.filter { bot ->
                        bot.name.startsWith(mentionQuery, ignoreCase = true) ||
                            bot.effectiveTitle.contains(mentionQuery, ignoreCase = true)
                    }
                }
            }
        if (filteredBots.isNotEmpty()) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    ),
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(filteredBots, key = { it.name }) { bot ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(bot) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BotAvatar(
                                name = bot.name,
                                avatar = bot.botMeta()?.avatar,
                                size = 28.dp,
                                showPresence = false,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "@${bot.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (bot.effectiveTitle != bot.name) {
                                    Text(
                                        text = bot.effectiveTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable attachment chip composable for showing a pending attachment
 * with a remove button.
 */
@Composable
fun AttachmentChip(
    attachment: Attachment,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnail = attachment.uri
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (attachment.isImage) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = attachment.name,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onPreview)
                            .testTag("attachment_preview"),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = attachment.name,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.chat_attach_remove_desc),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

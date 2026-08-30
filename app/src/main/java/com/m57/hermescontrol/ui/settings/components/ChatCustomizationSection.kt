package com.m57.hermescontrol.ui.settings.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m57.hermescontrol.R
import com.m57.hermescontrol.ui.chat.ChatPreferences
import com.m57.hermescontrol.ui.settings.SectionCard
import kotlinx.coroutines.launch

/**
 * Settings section for chat-surface customization (rtl-design-upgrade).
 *
 * Lets the user:
 *  - Pick a chat background image from the gallery (preview lives below).
 *  - Reset the background to the default theme.
 *  - Pick a custom color for the user-bubble.
 *  - Reset the bubble color to the theme primary.
 *  - Toggle the frosted-glass blur on the composer / top bar.
 *
 * The actual state lives in [ChatPreferences], which mirrors the encrypted
 * [com.m57.hermescontrol.data.local.AuthManager] store. Section changes
 * take effect immediately because every screen reads through the
 * CompositionLocals that [ChatPreferences] drives.
 */
@Composable
internal fun ChatCustomizationSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backgroundUri = ChatPreferences.backgroundUri.collectAsStateWithLifecycleValue()
    val userBubbleColorArgb = ChatPreferences.userBubbleColorArgb.collectAsStateWithLifecycleValue()
    val glassBlurEnabled = ChatPreferences.glassBlurEnabled.collectAsStateWithLifecycleValue()

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                // Persist URI access permission so the image keeps loading
                // across cold starts. Required for content:// URIs returned
                // by ACTION_GET_CONTENT on Android 11+.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                scope.launch { ChatPreferences.setBackgroundUri(uri.toString()) }
            }
        }

    SectionCard {
        Text(
            text = stringResource(R.string.settings_sec_chat_appearance),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── Background image ────────────────────────────────────────────
        Text(
            text = stringResource(R.string.settings_item_chat_background),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.settings_desc_chat_background),
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.weight(1f),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.settings_action_pick_image))
            }
            OutlinedButton(
                onClick = { ChatPreferences.setBackgroundUri(null) },
                enabled = !backgroundUri.isNullOrBlank(),
                modifier = Modifier.weight(1f),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.settings_action_reset))
            }
        }
        if (!backgroundUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        ),
            ) {
                coil3.compose.AsyncImage(
                    model =
                        coil3.request.ImageRequest.Builder(context)
                            .data(android.net.Uri.parse(backgroundUri))
                            .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                // Scrim overlay so the preview reads as "this is what you'll
                // see over your messages" (with the same dark scrim the chat
                // surface uses).
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                )
                Text(
                    text = stringResource(R.string.settings_chat_background_active),
                    color = MaterialTheme.colorScheme.onScrim,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── User-bubble color ──────────────────────────────────────────
        Text(
            text = stringResource(R.string.settings_item_user_bubble_color),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.settings_desc_user_bubble_color),
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BubbleColorSwatches(
            selectedArgb = userBubbleColorArgb,
            onPick = { ChatPreferences.setUserBubbleColorArgb(it) },
        )
        if (userBubbleColorArgb != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { ChatPreferences.setUserBubbleColorArgb(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_action_reset_to_theme))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Glass blur toggle ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_item_glass_blur),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_desc_glass_blur),
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
            Switch(
                checked = glassBlurEnabled,
                onCheckedChange = { ChatPreferences.setGlassBlurEnabled(it) },
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            )
        }
    }
}

/**
 * Curated palette of user-bubble colors plus a "theme default" entry.
 * Each swatch is a 36 dp circle with a check mark overlay when selected.
 */
@Composable
private fun BubbleColorSwatches(
    selectedArgb: Long?,
    onPick: (Long?) -> Unit,
) {
    val palette =
        listOf(
            null to "Theme",
            0xFF1E88E5L to "Ocean",
            0xFF43A047L to "Forest",
            0xFFE53935L to "Crimson",
            0xFF8E24AAL to "Royal",
            0xFFF57C00L to "Sunset",
            0xFF00897BL to "Teal",
            0xFF6D4C41L to "Cocoa",
            0xFF455A64L to "Slate",
        )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(palette) { (argb, label) ->
            val isSelected = argb == selectedArgb
            val swatchColor = argb?.let { Color(it.toULong()) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .let { base ->
                                if (swatchColor != null) {
                                    base.background(swatchColor)
                                } else {
                                    base.background(MaterialTheme.colorScheme.primary)
                                }
                            }.border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            ).clickable { onPick(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = label,
                            tint =
                                if (swatchColor?.let { it.luminance() > 0.5f } == true) {
                                    Color(red = 0f, green = 0f, blue = 0f)
                                } else {
                                    Color(red = 1f, green = 1f, blue = 1f)
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Small extension so we don't have to import collectAsStateWithLifecycle
// at the top of this file (kept local to avoid cluttering the call site).
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleValue(): T = collectAsStateWithLifecycle().value

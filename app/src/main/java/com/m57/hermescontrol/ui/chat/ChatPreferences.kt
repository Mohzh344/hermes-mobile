package com.m57.hermescontrol.ui.chat

import androidx.compose.runtime.compositionLocalOf
import com.m57.hermescontrol.data.local.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive view of the chat-surface user preferences — chat background URI,
 * user-bubble color, and the master frosted-glass blur toggle. Backed by
 * the encrypted `ServerStoreState` so the choices survive cold start
 * without us having to plumb a separate DataStore.
 *
 * Exposes plain Flows (no Compose dependency in this file) so the
 * CompositionLocal can hand them to composables without leaking
 * `MutableStateFlow` into the API surface.
 */
object ChatPreferences {
    private val _backgroundUri = MutableStateFlow(AuthManager.getChatBackgroundUri())
    val backgroundUri: StateFlow<String?> = _backgroundUri.asStateFlow()

    private val _userBubbleColorArgb = MutableStateFlow(AuthManager.getUserBubbleColorArgb())
    val userBubbleColorArgb: StateFlow<Long?> = _userBubbleColorArgb.asStateFlow()

    private val _glassBlurEnabled = MutableStateFlow(AuthManager.isGlassBlurEnabled())
    val glassBlurEnabled: StateFlow<Boolean> = _glassBlurEnabled.asStateFlow()

    fun setBackgroundUri(uri: String?) {
        AuthManager.setChatBackgroundUri(uri)
        _backgroundUri.value = uri
    }

    fun setUserBubbleColorArgb(argb: Long?) {
        AuthManager.setUserBubbleColorArgb(argb)
        _userBubbleColorArgb.value = argb
    }

    fun setGlassBlurEnabled(enabled: Boolean) {
        AuthManager.setGlassBlurEnabled(enabled)
        _glassBlurEnabled.value = enabled
    }
}

/**
 * CompositionLocal that carries the current user-bubble color override.
 * `null` means "use theme primary" — the bubble color picker in settings
 * writes to [ChatPreferences], which broadcasts via this local.
 */
val LocalUserBubbleColor = compositionLocalOf<Long?> { null }

/**
 * CompositionLocal for the chat background image URI (or null). Composable
 * callers read this and decide whether to layer a full-bleed image behind
 * the conversation timeline.
 */
val LocalChatBackgroundUri = compositionLocalOf<String?> { null }

package com.m57.hermescontrol.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * Helpers for per-string directionality (e.g. mixed Arabic + English).
 *
 * The host activity already applies the chosen language locale, so most of the
 * app inherits the right [LayoutDirection] for free. This file just centralises
 * the few places where we want to force a directionality regardless of the
 * system locale, or to detect a primarily-Arabic locale for explicit tweaks.
 */

/**
 * True when the current locale is primarily a right-to-left script
 * (Arabic, Hebrew, Persian, Urdu). Used to flip a few mirror-able icons
 * (chevrons, arrows) or to pad/align elements to the leading edge.
 */
val isRtlScript: Boolean
    get() = runCatching {
        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
        lang in setOf("ar", "he", "fa", "ur", "yi", "ps", "sd", "dv", "ks", "ckb")
    }.getOrDefault(false)

/**
 * CompositionLocal that exposes the resolved layout direction for the current
 * composition. Most callers should rely on [LocalLayoutDirection] instead,
 * but this indirection gives us a single override point if we ever want to
 * pin a screen to LTR (e.g. code blocks or technical content).
 */
val LocalResolvedLayoutDirection = compositionLocalOf { LayoutDirection.Ltr }

/**
 * Returns the [TextDirection] Compose should use for a *plain text* (not
 * Auto) element. Auto lets the engine detect a per-glyph direction, which
 * gives correct rendering for mixed Arabic/Latin text inside one paragraph.
 */
@Composable
fun rememberAutoTextDirection(): TextDirection = TextDirection.ContentOrLtr

/**
 * Strips bidi control characters from a string before it is displayed. Some
 * upstream APIs inject U+200E / U+200F markers that confuse Compose's bidi
 * algorithm; dropping them keeps alignment predictable.
 */
fun sanitizeBidi(text: String): String =
    text
        .replace("\u200E", "") // LRM
        .replace("\u200F", "") // RLM
        .replace("\u202A", "") // LRE
        .replace("\u202B", "") // RLE
        .replace("\u202C", "") // PDF
        .replace("\u202D", "") // LRO
        .replace("\u202E", "") // RLO
        .replace("\u2066", "") // LRI
        .replace("\u2067", "") // RLI
        .replace("\u2068", "") // FSI
        .replace("\u2069", "") // PDI

/**
 * Convenience: returns an empty [LocaleList] for components that don't need
 * the hint, or the system locale otherwise. Currently a thin wrapper to keep
 * imports localized to this file.
 */
@Suppress("unused")
fun defaultLocaleList(): androidx.compose.ui.text.intl.LocaleList =
    androidx.compose.ui.text.intl.LocaleList.current

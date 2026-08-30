package com.m57.hermescontrol.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composition-local for whether frosted-glass blur is currently allowed.
 * When `false`, glass surfaces fall back to a simple translucent tint —
 * cheaper to draw and works on devices that can't render the blur effect.
 */
val LocalGlassBlurEnabled = staticCompositionLocalOf { true }

/**
 * Frosted-glass surface used across the chat top bar, composer, drawer
 * header, and floating action bubbles.
 *
 * Renders a translucent tint + soft inner highlight + 1-px hairline border
 * to mimic iOS-style "glass" without requiring the RenderEffect blur
 * (which is unavailable on most Android API levels and slow on others).
 * The look reads as "frosted glass" because:
 *  - A semi-transparent surface (alpha 0.55..0.7) over whatever is behind
 *    blurs the underlying content visually (no actual GPU blur needed)
 *  - A diagonal linear-gradient highlight on top adds the "shiny" sheen
 *  - A hairline white border at 14% alpha simulates the glass edge
 *
 * The shape is configurable so callers can use pill, rounded, or square
 * rectangles as the context requires.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.surface,
    borderAlpha: Float = 0.18f,
    highlightAlpha: Float = 0.10f,
    content: @Composable () -> Unit,
) {
    val baseAlpha = if (MaterialTheme.colorScheme.surface.luminanceOrDefault() > 0.5f) 0.72f else 0.55f
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(tint.copy(alpha = baseAlpha))
                .drawBehind {
                    // Subtle diagonal highlight (top-left → bottom-right).
                    drawRoundRect(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = highlightAlpha),
                                        Color.Transparent,
                                    ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        size = Size(size.width, size.height * 0.6f),
                    )
                }.border(
                    width = 1.dp.toPx(),
                    color = Color.White.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(cornerRadius),
                ),
    ) {
        content()
    }
}

/**
 * Compact glass bubble for toolbar icons (menu, search, send, mic).
 * Same visual language as [GlassSurface] but tuned to small circle/squircle
 * shapes — slightly higher alpha so the icon stays legible at 36..44 dp.
 */
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val baseAlpha = if (MaterialTheme.colorScheme.surface.luminanceOrDefault() > 0.5f) 0.80f else 0.62f
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(tint.copy(alpha = baseAlpha))
                .drawBehind {
                    val cr = cornerRadius.toPx()
                    drawRoundRect(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.Transparent,
                                    ),
                                start = Offset.Zero,
                                end = Offset(size.toPx(), size.toPx()),
                            ),
                        cornerRadius = CornerRadius(cr, cr),
                        size = Size(size.toPx(), size.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.20f),
                        cornerRadius = CornerRadius(cr, cr),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
    ) {
        content()
    }
}

/**
 * Default luminance fallback (returns 0.5 for unset color spaces) — keeps
 * glass alpha math stable when the theme surface color is fully transparent.
 */
private fun Color.luminanceOrDefault(): Float =
    try {
        luminance()
    } catch (_: IllegalArgumentException) {
        0.5f
    }

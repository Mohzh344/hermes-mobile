package com.m57.hermescontrol.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * CompositionLocal that lets screen authors toggle the frosted-glass effect
 * at runtime (e.g. an accessibility "reduce effects" preference).
 *
 * Defaults to true — the entire app has been re-themed around glass surfaces,
 * so disabling it only takes a small per-screen override.
 */
val LocalGlassBlurEnabled = compositionLocalOf { true }

/**
 * A reusable glass surface used for floating bars, status pills, drawers and
 * modals. It renders a soft vertical gradient (matching the system status
 * bar scrim) plus a 1dp hairline border so the surface stays legible against
 * any chat background.
 *
 * The actual blur is performed by the host activity's `enableEdgeToEdge` +
 * Compose's automatic translucency; we only draw the tinted scrim here.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    paddingValues: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable () -> Unit,
) {
    val blur = LocalGlassBlurEnabled.current
    val cs = MaterialTheme.colorScheme
    val scrimTop = lerp(cs.surface, cs.onSurface.copy(alpha = 0.08f), 0.5f).copy(alpha = 0.78f)
    val scrimBottom = lerp(cs.surface, cs.onSurface.copy(alpha = 0.04f), 0.5f).copy(alpha = 0.68f)
    val border = cs.onSurface.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(scrimTop, scrimBottom),
                ),
            )
            .border(
                width = 1.dp,
                color = border,
                shape = shape,
            )
            .let { m -> if (blur) m else m.background(Color.Transparent) }
            .padding(paddingValues),
    ) {
        content()
    }
}

/**
 * Floating "status bar" pill that hugs the top of the screen a little
 * below the system status bar (default 8dp) so it never overlaps the
 * camera notch or the OS clock. Use it in place of [TopAppBar] in chat
 * screens for a softer, more iMessage-like feel.
 */
@Composable
fun FloatingTopBar(
    modifier: Modifier = Modifier,
    topGap: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = topGap, start = horizontalPadding, end = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = shape,
            paddingValues = PaddingValues(horizontal = 12.dp, vertical = verticalPadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) { content() }
        }
    }
}

/**
 * Compact fixed-height pill. Used for slim bars (e.g. a connection banner)
 * that should still feel like floating glass but only need one line.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    minHeight: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    GlassSurface(
        modifier = modifier.heightIn(min = minHeight),
        shape = RoundedCornerShape(18.dp),
        paddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    ) { content() }
}

/**
 * Linear-gradient height for a glass divider — used by the drawer's
 * sidebar edges to soften the transition between opaque and translucent
 * regions.
 */
fun glassDividerHeight(): Dp = min(1.dp, 1.dp)

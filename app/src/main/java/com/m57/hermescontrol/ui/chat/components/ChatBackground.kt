package com.m57.hermescontrol.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/**
 * Full-bleed chat background.
 *
 * Layered from back to front:
 *  1. Theme background (always).
 *  2. Optional user-picked image from the gallery (scrim to keep contrast
 *     with bubbles regardless of the photo brightness).
 *  3. A subtle vertical gradient overlay so bubbles near the top/bottom
 *     edges still have enough contrast to be readable.
 *
 * The user-picked URI is interpreted as both an Android content:// (gallery
 * picker) or file:// (when seeded from app-private storage). Coil's
 * AsyncImage resolves either transparently.
 */
@Composable
fun ChatBackground(
    imageUri: String?,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.32f,
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 2 — user-picked image, if any.
        if (!imageUri.isNullOrBlank()) {
            val model =
                remember(imageUri) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(imageUri))
                        .build()
                }
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Darken / lighten the image enough that text on top stays readable
            // regardless of the underlying photo's brightness.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        }

        // Layer 3 — top + bottom gradient so bubbles near the edges never
        // collide with status-bar / nav-bar icons.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.06f),
                                ),
                        ),
                    ),
        )
    }
}
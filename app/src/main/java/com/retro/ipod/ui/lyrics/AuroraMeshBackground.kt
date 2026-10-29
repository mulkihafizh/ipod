package com.retro.ipod.ui.lyrics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.retro.ipod.data.settings.BgMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic liquid Aurora Mesh & Blurred Artwork background matching foobar2000 Apple Music JS specification:
 * - Mode 0 (COVER): High-radius blurred album cover with 0.48 dark scrim overlay.
 * - Mode 1 (GRADIENT): Animated organic gradient mesh blobs from track palette with 0.35 dark overlay.
 */
@Composable
fun AuroraMeshBackground(
    modifier: Modifier = Modifier,
    bgMode: BgMode = BgMode.BLURRED_COVER,
    artworkFilePath: String? = null,
    coverBitmap: Bitmap? = null,
    scrimAlpha: Float = 0.48f // Matches CFG.bg.tint = 0.48 from reference JS
) {
    var resolvedBitmap by remember(artworkFilePath, coverBitmap) { mutableStateOf(coverBitmap) }

    // Resolve artwork from file path if bitmap not provided directly
    LaunchedEffect(artworkFilePath, coverBitmap) {
        if (coverBitmap != null) {
            resolvedBitmap = coverBitmap
        } else if (!artworkFilePath.isNullOrBlank()) {
            resolvedBitmap = com.retro.ipod.data.art.CoverArtResolver.loadBitmap(artworkFilePath)
        } else {
            resolvedBitmap = null
        }
    }

    // Default rich Apple Music Aurora colors
    var blobColor1 by remember { mutableStateOf(Color(0xFF4A148C)) } // Deep Violet
    var blobColor2 by remember { mutableStateOf(Color(0xFF00B0FF)) } // Electric Cyan
    var blobColor3 by remember { mutableStateOf(Color(0xFFFF4081)) } // Vibrant Coral
    var blobColor4 by remember { mutableStateOf(Color(0xFF00E676)) } // Emerald Glow

    // Extract palette dynamically if bitmap provided
    LaunchedEffect(resolvedBitmap) {
        val bmp = resolvedBitmap
        if (bmp != null) {
            try {
                Palette.from(bmp).generate { palette ->
                    if (palette != null) {
                        palette.dominantSwatch?.rgb?.let { blobColor1 = Color(it) }
                        palette.vibrantSwatch?.rgb?.let { blobColor2 = Color(it) }
                        palette.lightVibrantSwatch?.rgb?.let { blobColor3 = Color(it) }
                        palette.darkVibrantSwatch?.rgb?.let { blobColor4 = Color(it) }
                    }
                }
            } catch (_: Exception) {
                // Keep default vibrant colors
            }
        }
    }

    val currentBitmap = resolvedBitmap
    if (bgMode == BgMode.BLURRED_COVER && currentBitmap != null) {
        // Mode 0: Blurred cover art (aspect ratio preserved, centered fill) with 0.48 dark scrim
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF101014))) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(36.dp)
            )
            // Dark overlay tint matching CFG.bg.tint (0.48)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }
    } else {
        // Mode 1 (or Fallback if no cover art): Animated Gradient Mesh Blobs
        val infiniteTransition = rememberInfiniteTransition(label = "auroraTransition")

        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "auroraClock"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF101014))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val radius = (width.coerceAtLeast(height) * 0.7f)

                // Blob 1: Wandering top-left to center
                val x1 = width * 0.35f + (cos(time.toDouble()) * width * 0.15f).toFloat()
                val y1 = height * 0.30f + (sin(time.toDouble()) * height * 0.12f).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor1.copy(alpha = 0.70f), Color.Transparent),
                        center = Offset(x1, y1),
                        radius = radius
                    ),
                    center = Offset(x1, y1),
                    radius = radius
                )

                // Blob 2: Wandering top-right
                val x2 = width * 0.75f + (sin(time.toDouble() * 0.8) * width * 0.18f).toFloat()
                val y2 = height * 0.25f + (cos(time.toDouble() * 0.8) * height * 0.15f).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor2.copy(alpha = 0.65f), Color.Transparent),
                        center = Offset(x2, y2),
                        radius = radius * 0.95f
                    ),
                    center = Offset(x2, y2),
                    radius = radius * 0.95f
                )

                // Blob 3: Wandering bottom-left
                val x3 = width * 0.25f + (cos(time.toDouble() * 1.2) * width * 0.14f).toFloat()
                val y3 = height * 0.70f + (sin(time.toDouble() * 1.2) * height * 0.16f).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor3.copy(alpha = 0.60f), Color.Transparent),
                        center = Offset(x3, y3),
                        radius = radius * 0.9f
                    ),
                    center = Offset(x3, y3),
                    radius = radius * 0.9f
                )

                // Blob 4: Wandering bottom-right
                val x4 = width * 0.70f + (sin(time.toDouble() * 0.9) * width * 0.16f).toFloat()
                val y4 = height * 0.75f + (cos(time.toDouble() * 0.9) * height * 0.14f).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor4.copy(alpha = 0.60f), Color.Transparent),
                        center = Offset(x4, y4),
                        radius = radius * 0.9f
                    ),
                    center = Offset(x4, y4),
                    radius = radius * 0.9f
                )
            }

            // Dark contrast scrim to ensure high text legibility (CFG.bg.gradientTint = 0.35)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
    }
}

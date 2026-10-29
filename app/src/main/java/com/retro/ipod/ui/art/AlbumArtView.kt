package com.retro.ipod.ui.art

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Skeuomorphic iPod Album Art View.
 * Displays embedded ID3 cover art or procedural retro vinyl album art fallback.
 * Includes optional floor reflection (inverted duplicate with linear alpha mask).
 */
@Composable
fun AlbumArtView(
    modifier: Modifier = Modifier,
    filePath: String? = null,
    title: String? = null,
    artist: String? = null,
    size: Dp = 110.dp,
    reflectionHeight: Dp = 28.dp,
    withReflection: Boolean = false,
    cornerRadius: Dp = 4.dp,
    onClick: (() -> Unit)? = null
) {
    var embeddedArtBitmap by remember(filePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadAttempted by remember(filePath) { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        if (!filePath.isNullOrBlank()) {
            embeddedArtBitmap = com.retro.ipod.data.art.CoverArtResolver.loadBitmap(filePath)
            loadAttempted = true
        } else {
            embeddedArtBitmap = null
            loadAttempted = true
        }
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Cover Art with Realistic Soft Fading Drop Shadow
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // Diffuse Ambient Drop Shadow (low opacity, wide fade-out)
            Box(
                modifier = Modifier
                    .size(width = size + 8.dp, height = size + 10.dp)
                    .offset(y = 5.dp)
                    .clip(RoundedCornerShape(cornerRadius + 6.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Cover Container
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .border(0.5.dp, Color(0xFF383848).copy(alpha = 0.6f), RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF1A1A22)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = embeddedArtBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = title ?: "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Procedural Skeuomorphic Retro Vinyl Album Art Fallback
                    RetroVinylPlaceholder(
                        title = title ?: "iPod Classic",
                        artist = artist ?: "Audio Library",
                        size = size
                    )
                }

                // Glossy glass reflection overlay on the cover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.02f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Delicate Floor Reflection (mirrored bottom of album art fading into transparent)
        if (withReflection) {
            Box(
                modifier = Modifier
                    .width(size)
                    .height(reflectionHeight)
                    .clipToBounds()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0.40f),
                                0.35f to Color.White.copy(alpha = 0.18f),
                                0.70f to Color.White.copy(alpha = 0.05f),
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(cornerRadius))
                        .graphicsLayer {
                            scaleY = -1f
                        }
                ) {
                    val bmp = embeddedArtBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        RetroVinylPlaceholder(
                            title = title ?: "",
                            artist = artist ?: "",
                            size = size
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeuomorphic vinyl record / album sleeve procedural fallback.
 */
@Composable
fun RetroVinylPlaceholder(
    title: String,
    artist: String,
    size: Dp
) {
    // Generate deterministic hue from string
    val hash = (title + artist).hashCode()
    val baseColor = when (Math.abs(hash) % 4) {
        0 -> Color(0xFF0288D1) // iPod Blue
        1 -> Color(0xFFE91E63) // Magenta
        2 -> Color(0xFF00897B) // Teal
        else -> Color(0xFF673AB7) // Purple
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        baseColor.copy(alpha = 0.45f),
                        Color(0xFF141418),
                        Color(0xFF0C0C10)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Grooves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxR = this.size.minDimension / 2f
            for (i in 1..4) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = maxR * (i * 0.22f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }

        // Center vinyl label
        Box(
            modifier = Modifier
                .size(size * 0.45f)
                .clip(CircleShape)
                .background(baseColor)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title.take(8),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = artist.take(10),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 7.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

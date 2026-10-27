package com.retro.ipod.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.playback.CurrentPlaybackState
import com.retro.ipod.playback.audio.UsbDacState
import com.retro.ipod.ui.art.AlbumArtView
import com.retro.ipod.ui.theme.AppFonts
import com.retro.ipod.ui.theme.IpodScreenPalette

/**
 * Authentic iPod Classic Now Playing View (Spec Section 3, View 1).
 *
 * Features:
 * - Left column: 110dp Album Art with 28dp floor reflection. Tapping switches directly to Lyrics.
 * - Right column: Title, Artist, Album, 5-Star Rating, Track counter (x of y), Shuffle & Repeat controls.
 * - Bottom strip: Aqua progress bar with direct touch scrubbing and elapsed/remaining timestamps.
 */
@Composable
fun IpodNowPlayingScreen(
    playbackState: CurrentPlaybackState,
    currentPositionMs: Long,
    dacState: UsbDacState,
    onSeekTo: (Long) -> Unit,
    onOpenLyrics: () -> Unit,
    onDacBadgeClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE),
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onAddToPlaylist: (() -> Unit)? = null
) {
    val song = playbackState.currentSong
    var userRating by remember(song?.id) { mutableIntStateOf(4) } // Default 4 stars

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.nowPlayingBackground)
            .border(1.dp, palette.nowPlayingBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Main Area (Left Column: Art with Reflection, Right Column: Song Info)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column (48% width): 110dp Album Art with Floor Reflection
            Box(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtView(
                    filePath = song?.filePath,
                    title = song?.title ?: "iPod Classic",
                    artist = song?.artist ?: "Apple Inc.",
                    size = 110.dp,
                    reflectionHeight = 28.dp,
                    withReflection = true,
                    cornerRadius = 4.dp,
                    onClick = onOpenLyrics
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Column (52% width): Track Title, Artist, Album, Rating, Track Counter, Controls
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Track Counter (e.g. 14 of 639)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentIndex = if (playbackState.queueIndex >= 0) playbackState.queueIndex else playbackState.queue.indexOfFirst { it.id == song?.id }
                    val counterText = if (currentIndex >= 0 && playbackState.queue.isNotEmpty()) {
                        "${currentIndex + 1} of ${playbackState.queue.size}"
                    } else "Now Playing"

                    Text(
                        text = counterText,
                        color = palette.nowPlayingTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AppFonts.helvetica(FontWeight.Medium)
                    )

                    // Lyrics Quick Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0288D1).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable { onOpenLyrics() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "Lyrics",
                                tint = if (palette.theme == SkinTheme.CLASSIC_WHITE) Color(0xFF0288D1) else Color(0xFF4FC3F7),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "LYRICS",
                                color = if (palette.theme == SkinTheme.CLASSIC_WHITE) Color(0xFF0288D1) else Color(0xFF4FC3F7),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFonts.helvetica(FontWeight.Bold)
                            )
                        }
                    }
                }

                // Title & Artist & Album
                Column {
                    Text(
                        text = song?.title ?: "Select a Track",
                        color = palette.nowPlayingTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = AppFonts.helvetica(FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = song?.artist ?: "Unknown Artist",
                        color = palette.nowPlayingTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AppFonts.helvetica(FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song?.album ?: "Unknown Album",
                        color = palette.nowPlayingTextTertiary,
                        fontSize = 10.sp,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Interactive 5-Star Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (star in 1..5) {
                        val isFilled = star <= userRating
                        Icon(
                            imageVector = if (isFilled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Star $star",
                            tint = if (isFilled) palette.starFilled else palette.starEmpty,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { userRating = star }
                        )
                    }
                }

                // Playback Control Pills: Shuffle, Repeat, and Add to Playlist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Shuffle Pill
                    val isShuffle = playbackState.shuffleMode
                    val shuffleColor = if (isShuffle) {
                        if (palette.theme == SkinTheme.CLASSIC_WHITE) Color(0xFF0288D1) else Color.White
                    } else palette.nowPlayingTextTertiary

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isShuffle) Color(0xFF0288D1).copy(alpha = 0.35f) else palette.badgeBackground
                            )
                            .border(
                                1.dp,
                                if (isShuffle) Color(0xFF0288D1) else Color.Transparent,
                                RoundedCornerShape(3.dp)
                            )
                            .clickable { onToggleShuffle() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = shuffleColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isShuffle) "ON" else "OFF",
                            color = shuffleColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Repeat Pill
                    val isRepeatActive = playbackState.repeatMode != Player.REPEAT_MODE_OFF
                    val repeatIcon = when (playbackState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val repeatText = when (playbackState.repeatMode) {
                        Player.REPEAT_MODE_ALL -> "ALL"
                        Player.REPEAT_MODE_ONE -> "ONE"
                        else -> "OFF"
                    }
                    val repeatColor = if (isRepeatActive) {
                        if (palette.theme == SkinTheme.CLASSIC_WHITE) Color(0xFF0288D1) else Color.White
                    } else palette.nowPlayingTextTertiary

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isRepeatActive) Color(0xFF0288D1).copy(alpha = 0.35f) else palette.badgeBackground
                            )
                            .border(
                                1.dp,
                                if (isRepeatActive) Color(0xFF0288D1) else Color.Transparent,
                                RoundedCornerShape(3.dp)
                            )
                            .clickable { onCycleRepeat() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat",
                            tint = repeatColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = repeatText,
                            color = repeatColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Add to Playlist Pill
                    if (onAddToPlaylist != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.badgeBackground)
                                .clickable { onAddToPlaylist() }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = palette.nowPlayingTextTertiary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "+LIST",
                                color = palette.nowPlayingTextTertiary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Strip: iPod Aqua-Style Progress Bar with Direct Scrubbing
        IpodAquaScrubber(
            durationMs = playbackState.durationMs,
            currentPositionMs = currentPositionMs,
            palette = palette,
            onSeekTo = onSeekTo
        )
    }
}

/**
 * Skeuomorphic Aqua-Style Progress Bar with glossy reflection and direct touch scrubbing.
 */
@Composable
fun IpodAquaScrubber(
    durationMs: Long,
    currentPositionMs: Long,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE),
    onSeekTo: (Long) -> Unit
) {
    val duration = if (durationMs > 0) durationMs else 1L
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else {
        (currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Scrubber Canvas (Spec Section 3.1: Aqua gradient fill, glossy white top reflection line, direct drag)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekTo((fraction * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((dragFraction * duration).toLong())
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                val barWidth = size.width
                val barHeight = size.height
                val radius = barHeight / 2f

                // Track Background (Dark Inset Capsule)
                drawRoundRect(
                    color = palette.scrubberTrack,
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )

                // Track Inner Inset Border
                drawRoundRect(
                    color = palette.scrubberBorder,
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )

                // Active Aqua Filled Progress Bar
                val filledWidth = barWidth * activeFraction
                if (filledWidth > 0f) {
                    drawRoundRect(
                        brush = palette.scrubberProgress,
                        size = Size(filledWidth, barHeight),
                        cornerRadius = CornerRadius(radius, radius)
                    )

                    // Glossy White Top Reflection Line (Simulating Apple Aqua Gel)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(filledWidth, barHeight * 0.45f),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }

                // Thumb Indicator (Small Aqua Bead)
                if (filledWidth > 0f) {
                    drawCircle(
                        color = palette.scrubberThumb,
                        radius = radius * 0.8f,
                        center = Offset(filledWidth.coerceAtLeast(radius), barHeight / 2f)
                    )
                }
            }
        }

        // Timestamps (Elapsed & Remaining)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayMs = if (isDragging) (dragFraction * duration).toLong() else currentPositionMs
            val remainingMs = (duration - displayMs).coerceAtLeast(0L)

            Text(
                text = formatTime(displayMs),
                color = palette.nowPlayingTextTertiary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "-${formatTime(remainingMs)}",
                color = palette.nowPlayingTextTertiary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

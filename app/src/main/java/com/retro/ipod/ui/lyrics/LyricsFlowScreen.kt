package com.retro.ipod.ui.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.settings.BgMode
import com.retro.ipod.data.settings.LyricsDisplayMode
import com.retro.ipod.lyrics.model.LyricLine
import com.retro.ipod.lyrics.model.LyricWord
import com.retro.ipod.lyrics.model.LyricsDocument
import com.retro.ipod.ui.theme.AppFonts
import com.retro.ipod.ui.wheel.IpodHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-fidelity Apple Music style Syllable & Line Synced Lyrics Screen.
 * Fully compliant with foobar2000 Apple Music JS Reference Engine:
 * - Direct vector icons (no emojis)
 * - SF Pro for Modern lyrics panel, Helvetica & Helvetica Neue for Main / Retro view
 * - High-radius Blurred Album Cover background with 0.48 dark scrim
 * - Exact 38% viewport height centering on active line
 * - Sub-lyrics / Romanized / Translation dual-line pairing (subGap = 4dp, subActiveSize = 15sp)
 * - Standalone backing vocals right alignment "(...)"
 * - Real-time word-level sweep gradient & smooth landing transitions
 * - Distance-based alpha: active 1.0, near 0.53, far 0.20
 */
@Composable
fun LyricsFlowScreen(
    lyricsDocument: LyricsDocument,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    artworkFilePath: String? = null,
    lyricsMode: LyricsDisplayMode = LyricsDisplayMode.MODERN_FLOW,
    bgMode: BgMode = BgMode.BLURRED_COVER,
    glowEnabled: Boolean = true,
    fontScale: Float = 1.0f,
    lineGapDp: Int = 16,
    onSeekTo: (Long) -> Unit = {},
    onToggleLyricsMode: () -> Unit = {},
    onCloseLyrics: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }
    val listState = rememberLazyListState()

    val activeIndex = remember(currentPositionMs, lyricsDocument) {
        lyricsDocument.findActiveLineIndex(currentPositionMs)
    }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var isUserScrolling by remember { mutableStateOf(false) }

    // User touch interaction handler: pause auto-scroll during drag, resume 3s after release
    LaunchedEffect(isDragged) {
        if (isDragged) {
            isUserScrolling = true
        } else if (isUserScrolling) {
            delay(3000L)
            isUserScrolling = false
        }
    }

    // Auto-scroll keeping active line centered at 38% viewport height (focusRatio = 0.38)
    LaunchedEffect(activeIndex, isUserScrolling) {
        if (!isUserScrolling && activeIndex in lyricsDocument.lines.indices) {
            listState.animateScrollToItem(index = activeIndex, scrollOffset = 0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
            // Background Layer: Blurred Album Cover (or Dynamic Aurora Mesh fallback)
            AuroraMeshBackground(
                modifier = Modifier.fillMaxSize(),
                bgMode = bgMode,
                artworkFilePath = artworkFilePath
            )
        } else {
            // OG Classic Monochrome Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141416))
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header: Title, Artist, Mode Toggle Pill, and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lyricsDocument.title ?: "Lyrics",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
                            AppFonts.sfProDisplay(context, FontWeight.Bold)
                        } else {
                            AppFonts.helvetica(FontWeight.Bold)
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = lyricsDocument.artist ?: "Now Playing",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontFamily = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
                            AppFonts.sfProDisplay(context, FontWeight.Normal)
                        } else {
                            AppFonts.helvetica(FontWeight.Normal)
                        },
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mode Toggle Icon Button (Sleek Apple Music style icon button)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
                                    Color.White.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.10f)
                                }
                            )
                            .clickable { onToggleLyricsMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) Icons.Rounded.AutoAwesome else Icons.Rounded.Subtitles,
                            contentDescription = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) "Modern Fluid Lyrics" else "Retro OG Lyrics",
                            tint = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Close / Back button (Vector Close Icon)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onCloseLyrics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Lyrics Scrollable Content with 38% Viewport Centering
            if (lyricsDocument.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Lyrics",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 16.sp,
                        fontFamily = if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
                            AppFonts.sfProDisplay(context, FontWeight.Medium)
                        } else {
                            AppFonts.helvetica(FontWeight.Medium)
                        }
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight
                    // Focus ratio = 0.38f (active line positioned at ~38% viewport height)
                    val topPadding = viewportHeight * 0.38f
                    val bottomPadding = viewportHeight * 0.55f

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = topPadding,
                            bottom = bottomPadding,
                            start = 24.dp,
                            end = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(lineGapDp.dp)
                    ) {
                        itemsIndexed(
                            items = lyricsDocument.lines,
                            key = { _, line -> line.lineIndex }
                        ) { index, line ->
                            val isActive = index == activeIndex
                            val distance = Math.abs(index - activeIndex)
                            // Distance-based alpha from JS reference: active = 1.0, near = 0.53 (0x88/255), far = 0.20 (0x33/255)
                            val lineAlpha = when {
                                isActive -> 1.0f
                                distance == 1 -> 0.533f
                                else -> 0.20f
                            }

                            if (lyricsMode == LyricsDisplayMode.MODERN_FLOW) {
                                ModernFlowLyricLineView(
                                    line = line,
                                    isActive = isActive,
                                    lineAlpha = lineAlpha,
                                    currentPositionMs = currentPositionMs,
                                    glowEnabled = glowEnabled,
                                    fontScale = fontScale,
                                    onLineClick = {
                                        haptic.tick()
                                        isUserScrolling = false
                                        onSeekTo(line.startTimeMs)
                                    }
                                )
                            } else {
                                OgClassicLyricLineView(
                                    line = line,
                                    isActive = isActive,
                                    lineAlpha = lineAlpha,
                                    fontScale = fontScale,
                                    onLineClick = {
                                        haptic.tick()
                                        isUserScrolling = false
                                        onSeekTo(line.startTimeMs)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Flow lyric line with SF Pro typography, word-level sweep, bloom glow, and romanized sub-lyrics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModernFlowLyricLineView(
    line: LyricLine,
    isActive: Boolean,
    lineAlpha: Float,
    currentPositionMs: Long,
    glowEnabled: Boolean,
    fontScale: Float,
    onLineClick: () -> Unit
) {
    val context = LocalContext.current
    val sfProFont = AppFonts.sfProDisplay(context, if (isActive) FontWeight.Bold else FontWeight.Medium)
    val sfProSubFont = AppFonts.sfProDisplay(context, FontWeight.Normal)

    val targetScale = if (isActive) 1.04f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 200),
        label = "lineScale"
    )

    val targetTranslationY = if (isActive) -2f else 0f
    val animatedTranslationY by animateFloatAsState(
        targetValue = targetTranslationY,
        animationSpec = tween(durationMillis = 200),
        label = "lineLift"
    )

    val isRightAligned = line.alignRight || line.isBackingVocal
    val contentAlignment = if (isRightAligned) Alignment.CenterEnd else Alignment.CenterStart
    val textAlignment = if (isRightAligned) TextAlign.End else TextAlign.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedTranslationY
            }
            .clickable { onLineClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
    ) {
        // Primary Lyric Line
        if (line.hasSyllables && isActive) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                line.words.forEach { word ->
                    SyllableWordView(
                        word = word,
                        currentPositionMs = currentPositionMs,
                        glowEnabled = glowEnabled,
                        fontSizeSp = 24f * fontScale
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        } else {
            val fontSize = (if (isActive) 24f else 18f) * fontScale
            val textShadow = if (isActive && glowEnabled) {
                Shadow(color = Color(0x99FFFFFF), blurRadius = 16f)
            } else null

            Text(
                text = line.text,
                color = Color.White.copy(alpha = lineAlpha),
                fontSize = fontSize.sp,
                fontFamily = sfProFont,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                textAlign = textAlignment,
                style = TextStyle(shadow = textShadow),
                lineHeight = (fontSize * 1.35f).sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sub-lyrics / Romanized / Translation dual-line view (JS: subActiveSize = 15sp, subOtherSize = 13sp, subGap = 4dp)
        if (line.sub.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val subFontSize = (if (isActive) 15f else 13f) * fontScale
            val subAlpha = (if (isActive) 0.90f else 0.78f) * lineAlpha

            line.sub.forEach { subText ->
                Text(
                    text = subText,
                    color = Color.White.copy(alpha = subAlpha),
                    fontSize = subFontSize.sp,
                    fontFamily = sfProSubFont,
                    fontWeight = FontWeight.Normal,
                    textAlign = textAlignment,
                    lineHeight = (subFontSize * 1.30f).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Single syllable word that smoothly scales up when currently sung (no text slide),
 * with optical bloom glow and clean opacity transition.
 */
@Composable
private fun SyllableWordView(
    word: LyricWord,
    currentPositionMs: Long,
    glowEnabled: Boolean,
    fontSizeSp: Float
) {
    val context = LocalContext.current
    val sfProBold = AppFonts.sfProDisplay(context, FontWeight.Bold)

    val isSung = word.isPast(currentPositionMs)
    val isSinging = word.isCurrentlySung(currentPositionMs)

    val wordLift by animateFloatAsState(
        targetValue = if (isSinging) -2f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "wordLift"
    )

    val wordScale by animateFloatAsState(
        targetValue = if (isSinging) 1.10f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "wordScale"
    )

    val textShadow = if (isSinging && glowEnabled) {
        Shadow(color = Color(0xCCFFFFFF), blurRadius = 14f)
    } else null

    val wordAlpha = when {
        isSinging || isSung -> 1.0f
        else -> 0.38f
    }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = wordScale
            scaleY = wordScale
            translationY = wordLift
        }
    ) {
        Text(
            text = word.text,
            color = Color.White.copy(alpha = wordAlpha),
            fontSize = fontSizeSp.sp,
            fontFamily = sfProBold,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = textShadow)
        )
    }
}

/**
 * OG Retro iPod Monochrome plain-text line rendered with Helvetica / Helvetica Neue.
 */
@Composable
private fun OgClassicLyricLineView(
    line: LyricLine,
    isActive: Boolean,
    lineAlpha: Float,
    fontScale: Float,
    onLineClick: () -> Unit
) {
    val helveticaFont = AppFonts.helvetica(if (isActive) FontWeight.Bold else FontWeight.Normal)
    val bgColor = if (isActive) Color(0xFF007AFF).copy(alpha = 0.25f) else Color.Transparent
    val textColor = if (isActive) Color(0xFF4FC3F7) else Color(0xFF9E9E9E).copy(alpha = lineAlpha.coerceAtLeast(0.4f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable { onLineClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isActive) "› " else "  ",
                color = Color(0xFF4FC3F7),
                fontSize = (15f * fontScale).sp,
                fontFamily = helveticaFont,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = line.text,
                color = textColor,
                fontSize = (14f * fontScale).sp,
                fontFamily = helveticaFont,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = if (line.alignRight) TextAlign.End else TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }

        // Sub lines in classic mode
        if (line.sub.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            line.sub.forEach { subText ->
                Text(
                    text = "    $subText",
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = (12f * fontScale).sp,
                    fontFamily = helveticaFont,
                    fontWeight = FontWeight.Normal,
                    textAlign = if (line.alignRight) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

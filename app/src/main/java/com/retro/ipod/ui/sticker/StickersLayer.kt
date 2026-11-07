package com.retro.ipod.ui.sticker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.sticker.IpodSticker
import com.retro.ipod.ui.wheel.IpodHapticFeedback
import kotlin.math.roundToInt

/**
 * Interactive Stickers Canvas Layer rendered behind the hardware Click Wheel and screen frame.
 * Gestures (pan, pinch-scale, 2-finger rotate) are ONLY ACTIVE when isEditMode is true.
 * When isEditMode is false, stickers are locked in place and ignore all touch inputs.
 */
@Composable
fun StickersLayer(
    stickers: List<IpodSticker>,
    onUpdateSticker: (IpodSticker) -> Unit,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val containerWidthPx = containerSize.width.toFloat()
            val containerHeightPx = containerSize.height.toFloat()
            val baseSizeDp = 76.dp

            stickers.take(3).forEach { sticker ->
                DraggableStickerItem(
                    key = sticker.id,
                    sticker = sticker,
                    baseSizeDp = baseSizeDp,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    isEditMode = isEditMode,
                    onUpdateSticker = onUpdateSticker
                )
            }
        }
    }
}

@Composable
private fun DraggableStickerItem(
    key: String,
    sticker: IpodSticker,
    baseSizeDp: Dp,
    containerWidthPx: Float,
    containerHeightPx: Float,
    isEditMode: Boolean,
    onUpdateSticker: (IpodSticker) -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }

    // Mutable states that accumulate pan, zoom, and rotation smoothly without resetting
    var currentNormX by remember(key) { mutableFloatStateOf(sticker.normalizedX) }
    var currentNormY by remember(key) { mutableFloatStateOf(sticker.normalizedY) }
    var currentScale by remember(key) { mutableFloatStateOf(sticker.scale) }
    var currentRotation by remember(key) { mutableFloatStateOf(sticker.rotation) }

    // Threshold breakout mechanics
    val normalMaxThreshold = 2.4f
    val absoluteMaxScale = 8.5f
    val absoluteMinScale = 0.35f

    var isThresholdBroken by remember(key) { mutableStateOf(sticker.scale > normalMaxThreshold) }
    var overpullAccumulator by remember(key) { mutableFloatStateOf(0f) }

    // Synchronize if modified externally (e.g. from Settings size cycling, rotation button, or reset pos)
    LaunchedEffect(sticker.normalizedX, sticker.normalizedY, sticker.scale, sticker.rotation) {
        currentNormX = sticker.normalizedX
        currentNormY = sticker.normalizedY
        currentScale = sticker.scale
        currentRotation = sticker.rotation
        if (sticker.scale > normalMaxThreshold) {
            isThresholdBroken = true
        }
    }

    val stickerSizeDp = baseSizeDp * currentScale
    val stickerSizePx = with(density) { stickerSizeDp.toPx() }
    val isHuge = currentScale > normalMaxThreshold

    Box(
        modifier = Modifier
            .offset {
                val leftPx = (currentNormX * containerWidthPx) - (stickerSizePx / 2f)
                val topPx = (currentNormY * containerHeightPx) - (stickerSizePx / 2f)
                IntOffset(
                    leftPx.roundToInt(),
                    topPx.roundToInt()
                )
            }
            .size(stickerSizeDp)
            .graphicsLayer {
                rotationZ = currentRotation
            }
            .then(
                if (isEditMode) {
                    Modifier
                        .border(
                            width = if (isHuge) 2.dp else 1.5.dp,
                            color = if (isHuge) Color(0xFFFFB300) else Color(0x994FC3F7),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(2.dp)
                        .pointerInput(key) {
                            detectTransformGestures { _, pan, zoom, rotationDelta ->
                                // Dynamic bounds allowing huge stickers to be positioned artistically bleeding past edges
                                val extraMargin = ((currentScale - 1.0f) * 0.35f).coerceAtLeast(0f)
                                val minX = 0.04f - extraMargin
                                val maxX = 0.96f + extraMargin
                                val minY = 0.04f - extraMargin
                                val maxY = 0.96f + extraMargin

                                // 1-Finger / 2-Finger continuous drag
                                currentNormX = (currentNormX + (pan.x / containerWidthPx)).coerceIn(minX, maxX)
                                currentNormY = (currentNormY + (pan.y / containerHeightPx)).coerceIn(minY, maxY)

                                // 2-Finger continuous pinch-to-scale with Threshold Breakout
                                if (isThresholdBroken) {
                                    val newScale = (currentScale * zoom).coerceIn(absoluteMinScale, absoluteMaxScale)
                                    if (newScale < normalMaxThreshold) {
                                        // User pinched back down into normal scale range
                                        isThresholdBroken = false
                                        overpullAccumulator = 0f
                                        haptic.click()
                                    }
                                    currentScale = newScale
                                } else {
                                    val candidateScale = currentScale * zoom
                                    if (candidateScale <= normalMaxThreshold) {
                                        currentScale = candidateScale.coerceAtLeast(absoluteMinScale)
                                        overpullAccumulator = 0f
                                    } else {
                                        // Pushing against the normal threshold barrier: require intentional overpull
                                        if (zoom > 1.0f) {
                                            overpullAccumulator += (zoom - 1.0f)
                                            // Subtle elastic stretch tension before snap
                                            val stretch = (overpullAccumulator * 0.4f).coerceAtMost(0.2f)
                                            currentScale = normalMaxThreshold + stretch

                                            if (overpullAccumulator >= 0.18f) {
                                                // SNAP! Threshold broken into huge breakout mode!
                                                isThresholdBroken = true
                                                overpullAccumulator = 0f
                                                haptic.heavyClick()
                                                currentScale = normalMaxThreshold + 0.35f
                                            }
                                        }
                                    }
                                }

                                // 2-Finger continuous rotation in degrees
                                currentRotation = (currentRotation + rotationDelta) % 360f

                                onUpdateSticker(
                                    sticker.copy(
                                        normalizedX = currentNormX,
                                        normalizedY = currentNormY,
                                        scale = currentScale,
                                        rotation = currentRotation
                                    )
                                )
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        StickerGraphic(
            sticker = sticker.copy(scale = currentScale, rotation = currentRotation),
            baseSize = stickerSizeDp
        )

        // In edit mode, when broken into huge scale, display glowing breakout badge
        if (isEditMode && isHuge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xE6FF8F00))
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⚡ ${(currentScale * 100).toInt()}%",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

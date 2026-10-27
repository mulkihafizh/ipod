package com.retro.ipod.ui.sticker

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.retro.ipod.data.sticker.IpodSticker
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

    // Mutable states that accumulate pan, zoom, and rotation smoothly without resetting
    var currentNormX by remember(key) { mutableFloatStateOf(sticker.normalizedX) }
    var currentNormY by remember(key) { mutableFloatStateOf(sticker.normalizedY) }
    var currentScale by remember(key) { mutableFloatStateOf(sticker.scale) }
    var currentRotation by remember(key) { mutableFloatStateOf(sticker.rotation) }

    // Synchronize if modified externally (e.g. from Settings size cycling, rotation button, or reset pos)
    LaunchedEffect(sticker.normalizedX, sticker.normalizedY, sticker.scale, sticker.rotation) {
        currentNormX = sticker.normalizedX
        currentNormY = sticker.normalizedY
        currentScale = sticker.scale
        currentRotation = sticker.rotation
    }

    val stickerSizeDp = baseSizeDp * currentScale
    val stickerSizePx = with(density) { stickerSizeDp.toPx() }

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
                            1.5.dp,
                            Color(0x994FC3F7),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(2.dp)
                        .pointerInput(key) {
                            detectTransformGestures { _, pan, zoom, rotationDelta ->
                                // 1-Finger / 2-Finger continuous drag
                                currentNormX = (currentNormX + (pan.x / containerWidthPx)).coerceIn(0.04f, 0.96f)
                                currentNormY = (currentNormY + (pan.y / containerHeightPx)).coerceIn(0.04f, 0.96f)
                                // 2-Finger continuous pinch-to-scale
                                currentScale = (currentScale * zoom).coerceIn(0.4f, 2.5f)
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
    }
}

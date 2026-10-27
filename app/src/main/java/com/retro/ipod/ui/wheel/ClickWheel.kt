package com.retro.ipod.ui.wheel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.ui.theme.AppFonts
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Authentic iPod Classic Click Wheel Composable.
 *
 * Implements polar coordinate calculation via atan2(y, x), rotary step accumulation
 * with 18-degree thresholds (20 ticks per 360-degree rotation), mechanical haptic feedback,
 * 4 quadrant perimeter switches, and center selection button.
 */
@Composable
fun ClickWheel(
    modifier: Modifier = Modifier,
    wheelSize: Dp = 230.dp,
    centerButtonSize: Dp = 82.dp,
    theme: SkinTheme = SkinTheme.CLASSIC_WHITE,
    hapticEnabled: Boolean = true,
    onRotateStep: (isClockwise: Boolean) -> Unit = {},
    onCenterClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPrevClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }
    haptic.isEnabled = hapticEnabled

    // Theme color palettes
    val (outerWheelBrush, centerButtonBrush, borderColor, textColor) = when (theme) {
        SkinTheme.U2_EDITION -> QuadWheelColors(
            outerBrush = Brush.radialGradient(listOf(Color(0xFFE53935), Color(0xFFC62828))),
            centerBrush = Brush.radialGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))),
            border = Color(0xFFB71C1C),
            text = Color(0xFFFFFFFF)
        )
        SkinTheme.BRUSHED_ALUMINUM -> QuadWheelColors(
            outerBrush = Brush.radialGradient(listOf(Color(0xFF323236), Color(0xFF252528))),
            centerBrush = Brush.radialGradient(listOf(Color(0xFF404044), Color(0xFF2C2C30))),
            border = Color(0xFF48484A),
            text = Color(0xFFE0E0E0)
        )
        else -> QuadWheelColors( // CLASSIC_WHITE
            outerBrush = Brush.radialGradient(listOf(Color(0xFFF9F9FA), Color(0xFFEDEDF0))),
            centerBrush = Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF2F2F4))),
            border = Color(0xFFD1D1D6),
            text = Color(0xFF757575)
        )
    }

    var accumulatedAngleDelta by remember { mutableFloatStateOf(0f) }
    var previousAngleDegrees by remember { mutableFloatStateOf(0f) }

    val centerInteractionSource = remember { MutableInteractionSource() }
    val isCenterPressed by centerInteractionSource.collectIsPressedAsState()
    val centerScale by animateFloatAsState(
        targetValue = if (isCenterPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "centerButtonScale"
    )

    Box(
        modifier = modifier
            .size(wheelSize)
            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(outerWheelBrush)
            .border(1.dp, borderColor, CircleShape)
            .pointerInput(Unit) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val centerRadiusPx = (centerButtonSize.toPx()) / 2f
                val outerRadiusPx = (wheelSize.toPx()) / 2f

                detectDragGestures(
                    onDragStart = { startOffset ->
                        val dx = startOffset.x - centerOffset.x
                        val dy = startOffset.y - centerOffset.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist in centerRadiusPx..outerRadiusPx) {
                            previousAngleDegrees = (atan2(dy, dx) * 180f / PI.toFloat())
                            accumulatedAngleDelta = 0f
                        }
                    },
                    onDrag = { change, _ ->
                        val currentOffset = change.position
                        val dx = currentOffset.x - centerOffset.x
                        val dy = currentOffset.y - centerOffset.y
                        val dist = sqrt(dx * dx + dy * dy)

                        // Only track rotation in the annular ring outside center button
                        if (dist in centerRadiusPx..outerRadiusPx) {
                            val currentAngle = (atan2(dy, dx) * 180f / PI.toFloat())
                            var delta = currentAngle - previousAngleDegrees

                            // Normalize angle wrap across -180 / +180 boundary
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            accumulatedAngleDelta += delta
                            previousAngleDegrees = currentAngle

                            // 18 degrees per tick (20 discrete ticks per 360-degree rotation)
                            val stepThreshold = 18f
                            if (accumulatedAngleDelta >= stepThreshold) {
                                val steps = (accumulatedAngleDelta / stepThreshold).toInt()
                                repeat(steps) {
                                    onRotateStep(true) // Clockwise (scroll down)
                                    haptic.tick()
                                }
                                accumulatedAngleDelta %= stepThreshold
                            } else if (accumulatedAngleDelta <= -stepThreshold) {
                                val steps = (-accumulatedAngleDelta / stepThreshold).toInt()
                                repeat(steps) {
                                    onRotateStep(false) // Counter-clockwise (scroll up)
                                    haptic.tick()
                                }
                                accumulatedAngleDelta %= stepThreshold
                            }
                        }
                    },
                    onDragEnd = {
                        accumulatedAngleDelta = 0f
                    },
                    onDragCancel = {
                        accumulatedAngleDelta = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle annular groove ring dividing outer ring from center
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = (centerButtonSize.toPx() / 2f) + 1.dp.toPx()
            drawCircle(
                color = borderColor.copy(alpha = 0.5f),
                radius = r,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // ==========================================
        // Perimeter Quadrant Buttons
        // ==========================================

        // Top: MENU
        ClickWheelQuadrantButton(
            text = "MENU",
            textColor = textColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 14.dp),
            onClick = {
                haptic.click()
                onMenuClick()
            }
        )

        // Bottom: PLAY / PAUSE
        ClickWheelQuadrantButton(
            text = "▶❚❚",
            textColor = textColor,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-14).dp),
            onClick = {
                haptic.click()
                onPlayPauseClick()
            }
        )

        // Left: PREVIOUS
        ClickWheelQuadrantButton(
            text = "|◀◀",
            textColor = textColor,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 12.dp),
            onClick = {
                haptic.click()
                onPrevClick()
            }
        )

        // Right: NEXT
        ClickWheelQuadrantButton(
            text = "▶▶|",
            textColor = textColor,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp),
            onClick = {
                haptic.click()
                onNextClick()
            }
        )

        // ==========================================
        // Circular Center Button
        // ==========================================
        Box(
            modifier = Modifier
                .size(centerButtonSize)
                .scale(centerScale)
                .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(centerButtonBrush)
                .border(1.dp, borderColor, CircleShape)
                .clickable(
                    interactionSource = centerInteractionSource,
                    indication = null
                ) {
                    haptic.heavyClick()
                    onCenterClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle bevel ring on center button surface
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bevelRadius = (size.minDimension / 2f) - 2.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = bevelRadius,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun ClickWheelQuadrantButton(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = 60),
        label = "quadrantButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPressed) textColor.copy(alpha = 0.5f) else textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFonts.helvetica(FontWeight.Bold),
            letterSpacing = 0.5.sp
        )
    }
}

private data class QuadWheelColors(
    val outerBrush: Brush,
    val centerBrush: Brush,
    val border: Color,
    val text: Color
)

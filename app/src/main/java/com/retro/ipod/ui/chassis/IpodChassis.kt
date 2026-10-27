package com.retro.ipod.ui.chassis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retro.ipod.data.settings.SkinTheme

/**
 * Skeuomorphic iPod Chassis Container implementing Section 1.2 of the iPod UI/UX Specification.
 *
 * Wraps the 4:3 Mini Display LCD and the Click Wheel inside a beveled hardware body
 * matching the selected Skin Theme (Classic White, U2 Edition, or Brushed Aluminum).
 */
@Composable
fun IpodChassis(
    theme: SkinTheme,
    screenContent: @Composable () -> Unit,
    wheelContent: @Composable () -> Unit,
    stickersContent: (@Composable () -> Unit)? = null,
    overlayContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chassisColors = when (theme) {
        SkinTheme.U2_EDITION -> ChassisThemeColors(
            background = Brush.verticalGradient(listOf(Color(0xFF1E1E22), Color(0xFF121214), Color(0xFF0C0C0E))),
            chamferBorder = Color(0xFF2C2C34),
            innerBezel = Color(0xFF060608),
            bezelBorder = Color(0xFF280808)
        )
        SkinTheme.BRUSHED_ALUMINUM -> ChassisThemeColors(
            background = Brush.verticalGradient(listOf(Color(0xFF34343A), Color(0xFF24242A), Color(0xFF1C1C20))),
            chamferBorder = Color(0xFF484852),
            innerBezel = Color(0xFF101014),
            bezelBorder = Color(0xFF050508)
        )
        else -> ChassisThemeColors( // CLASSIC_WHITE
            background = Brush.verticalGradient(listOf(Color(0xFFF7F7F9), Color(0xFFEDEDF0), Color(0xFFE2E2E6))),
            chamferBorder = Color(0xFFC7C7CC),
            innerBezel = Color(0xFFD4D4DC),
            bezelBorder = Color(0xFFB8B8C0)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E)), // Ambient outer viewport backdrop
        contentAlignment = Alignment.Center
    ) {
        // iPod Beveled Hardware Chassis Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.98f)
                .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(28.dp))
                .background(chassisColors.background)
                .border(1.5.dp, chassisColors.chamferBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Layer 1: Draggable & Resizable Stickers (rendered behind hardware controls & screen)
            stickersContent?.invoke()

            // Layer 2: Hardware Faceplate Components (Screen & Click Wheel)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Upper Section: Authentic 4:3 LCD Mini Display Area with physical inset recession
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.72f) // Reduced screen height for authentic iPod proportions and sticker placement
                        .clip(RoundedCornerShape(12.dp))
                        .background(chassisColors.innerBezel)
                        .border(2.dp, chassisColors.bezelBorder, RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    screenContent()
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lower Section: Tactile Click Wheel Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.68f)
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    wheelContent()
                }
            }

            // Layer 3: Floating Faceplate Overlay (e.g. Sticker Edit Mode HUD)
            overlayContent?.invoke()
        }
    }
}

private data class ChassisThemeColors(
    val background: Brush,
    val chamferBorder: Color,
    val innerBezel: Color,
    val bezelBorder: Color
)

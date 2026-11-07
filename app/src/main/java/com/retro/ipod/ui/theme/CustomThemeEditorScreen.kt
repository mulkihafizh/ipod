package com.retro.ipod.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.ui.wheel.IpodHapticFeedback

data class NamedColorOption(
    val name: String,
    val colorLong: Long
)

/**
 * Inside-the-screen Custom Color Theme Studio.
 * Allows users to choose their own chassis skin-color, wheel color, and toggle between
 * pre-determined Light or Dark LCD screen/font colorways.
 */
@Composable
fun CustomThemeEditorScreen(
    currentSkinColor: Long,
    currentWheelColor: Long,
    currentScreenIsDark: Boolean,
    palette: IpodScreenPalette,
    onSkinColorChange: (Long) -> Unit,
    onWheelColorChange: (Long) -> Unit,
    onScreenModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }
    val verticalScroll = rememberScrollState()

    val skinColorOptions = remember {
        listOf(
            NamedColorOption("Cobalt Blue", 0xFF1976D2L),
            NamedColorOption("Classic White", 0xFFF5F5F7L),
            NamedColorOption("Jet Black", 0xFF18181CL),
            NamedColorOption("18 Pro Burgundy", 0xFF4D1821L),
            NamedColorOption("Deep Burgundy", 0xFF880E4FL),
            NamedColorOption("Space Gray", 0xFF3A3A40L),
            NamedColorOption("Product Red", 0xFFD32F2FL),
            NamedColorOption("Rose Gold", 0xFFE89BA5L),
            NamedColorOption("Forest Green", 0xFF2E7D32L),
            NamedColorOption("Purple", 0xFF6A1B9AL),
            NamedColorOption("Pastel Cyan", 0xFF00ACC1L),
            NamedColorOption("Sunset Orange", 0xFFE65100L),
            NamedColorOption("Canary Yellow", 0xFFF57F17L)
        )
    }

    val wheelColorOptions = remember {
        listOf(
            NamedColorOption("Porcelain White", 0xFFFFFFFFL),
            NamedColorOption("Platinum Silver", 0xFFE0E0E6L),
            NamedColorOption("Stealth Black", 0xFF212124L),
            NamedColorOption("18 Pro Burgundy", 0xFF4D1821L),
            NamedColorOption("Deep Burgundy", 0xFF880E4FL),
            NamedColorOption("Charcoal Gray", 0xFF37474FL),
            NamedColorOption("Crimson Red", 0xFFC62828L),
            NamedColorOption("Royal Navy", 0xFF0D47A1L)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.screenBackground)
            .border(1.dp, palette.screenBorder, RoundedCornerShape(8.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(palette.titleBarBrush)
                .clickable { onBack() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = palette.chevronColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "Custom Theme Studio",
                    color = palette.titleBarText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.dividerColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.menuLeftBackground)
                .verticalScroll(verticalScroll)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section 1: Chassis / Skin Color
            Column {
                Text(
                    text = "1. CHASSIS SKIN COLOR",
                    color = palette.settingsItemValue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                val skinScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(skinScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skinColorOptions.forEach { opt ->
                        val isSelected = opt.colorLong == currentSkinColor
                        ColorSwatchItem(
                            option = opt,
                            isSelected = isSelected,
                            palette = palette,
                            onClick = {
                                haptic.tick()
                                onSkinColorChange(opt.colorLong)
                            }
                        )
                    }
                }
            }

            // Section 2: Click Wheel Color
            Column {
                Text(
                    text = "2. CLICK WHEEL COLOR",
                    color = palette.settingsItemValue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                val wheelScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(wheelScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wheelColorOptions.forEach { opt ->
                        val isSelected = opt.colorLong == currentWheelColor
                        ColorSwatchItem(
                            option = opt,
                            isSelected = isSelected,
                            palette = palette,
                            onClick = {
                                haptic.tick()
                                onWheelColorChange(opt.colorLong)
                            }
                        )
                    }
                }
            }

            // Section 3: Screen & Font Colors (Light or Dark)
            Column {
                Text(
                    text = "3. SCREEN & FONT PALETTE",
                    color = palette.settingsItemValue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Light LCD Option
                    ScreenModeCard(
                        title = "Light LCD",
                        subtitle = "Classic Monochrome & Dark Font",
                        isDark = false,
                        isSelected = !currentScreenIsDark,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.tick()
                            onScreenModeChange(false)
                        }
                    )

                    // Dark LCD Option
                    ScreenModeCard(
                        title = "Dark LCD",
                        subtitle = "Stealth Modern & Crisp White Font",
                        isDark = true,
                        isSelected = currentScreenIsDark,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.tick()
                            onScreenModeChange(true)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Changes take effect live on the iPod casing & screen",
                color = palette.itemSubtitle,
                fontSize = 8.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ColorSwatchItem(
    option: NamedColorOption,
    isSelected: Boolean,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    val swatchColor = Color(option.colorLong)

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    if (isSelected) 2.5.dp else 1.dp,
                    if (isSelected) palette.settingsItemValue else Color.Black.copy(alpha = 0.25f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Determine icon color based on swatch brightness
                val isBright = (swatchColor.red * 0.299f + swatchColor.green * 0.587f + swatchColor.blue * 0.114f) > 0.5f
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = if (isBright) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = option.name,
            color = if (isSelected) palette.itemText else palette.itemSubtitle,
            fontSize = 7.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun ScreenModeCard(
    title: String,
    subtitle: String,
    isDark: Boolean,
    isSelected: Boolean,
    palette: IpodScreenPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardBg = if (isSelected) palette.selectionGradient else palette.titleBarBrush
    val cardBorder = if (isSelected) palette.settingsItemValue else palette.screenBorder

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(cardBg)
            .border(if (isSelected) 1.5.dp else 1.dp, cardBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = title,
                tint = if (isSelected) palette.selectionText else palette.itemText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) palette.selectionText else palette.itemText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = subtitle,
            color = if (isSelected) palette.selectionText.copy(alpha = 0.85f) else palette.itemSubtitle,
            fontSize = 7.sp,
            maxLines = 2
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isDark) Color(0xFF141418) else Color(0xFFF2F2F7))
                .border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
        )
    }
}


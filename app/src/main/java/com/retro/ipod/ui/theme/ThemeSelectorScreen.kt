package com.retro.ipod.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.ui.wheel.IpodHapticFeedback

data class ThemeCardModel(
    val theme: SkinTheme,
    val title: String,
    val subtitle: String,
    val chassisColor: Color,
    val wheelColor: Color,
    val isScreenDark: Boolean
)

/**
 * Modern in-screen horizontal Theme Selector.
 * Replaces plain vertical text list with visual mini-swatches and live preview inside the iPod LCD.
 */
@Composable
fun ThemeSelectorScreen(
    currentTheme: SkinTheme,
    customSkinColor: Color,
    customWheelColor: Color,
    customScreenIsDark: Boolean,
    highlightedIndex: Int,
    palette: IpodScreenPalette,
    onSelectTheme: (SkinTheme) -> Unit,
    onOpenCustomizer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }
    val lazyRowState = rememberLazyListState()

    val themes = remember(customSkinColor, customWheelColor, customScreenIsDark) {
        listOf(
            ThemeCardModel(
                theme = SkinTheme.CLASSIC_WHITE,
                title = "Classic White",
                subtitle = "Original 2001 Porcelain",
                chassisColor = Color(0xFFEDEDF0),
                wheelColor = Color(0xFFF9F9FA),
                isScreenDark = false
            ),
            ThemeCardModel(
                theme = SkinTheme.U2_EDITION,
                title = "U2 Special",
                subtitle = "Matte Black & Red Wheel",
                chassisColor = Color(0xFF141416),
                wheelColor = Color(0xFFE53935),
                isScreenDark = true
            ),
            ThemeCardModel(
                theme = SkinTheme.BRUSHED_ALUMINUM,
                title = "Aluminum",
                subtitle = "Anodized Space Gray",
                chassisColor = Color(0xFF2E2E34),
                wheelColor = Color(0xFF404046),
                isScreenDark = true
            ),
            ThemeCardModel(
                theme = SkinTheme.CUSTOM,
                title = "Custom Colors",
                subtitle = "Custom Wheel & Chassis",
                chassisColor = customSkinColor,
                wheelColor = customWheelColor,
                isScreenDark = customScreenIsDark
            )
        )
    }

    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex in themes.indices) {
            lazyRowState.animateScrollToItem(highlightedIndex)
        }
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
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBack() }
                ) {
                    Text(
                        text = "‹",
                        color = palette.chevronColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Skin & Chassis Themes",
                        color = palette.titleBarText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentTheme == SkinTheme.CUSTOM) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.badgeBackground)
                            .clickable { onOpenCustomizer() }
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "CUSTOMIZE ›",
                            color = palette.settingsItemValue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.dividerColor)
        )

        // Horizontal Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.menuLeftBackground),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyRow(
                state = lazyRowState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(themes, key = { _, item -> item.theme.name }) { index, item ->
                    val isHighlighted = index == highlightedIndex
                    val isSelected = currentTheme == item.theme

                    ThemePreviewCard(
                        model = item,
                        isHighlighted = isHighlighted,
                        isSelected = isSelected,
                        palette = palette,
                        onClick = {
                            haptic.tick()
                            onSelectTheme(item.theme)
                            if (item.theme == SkinTheme.CUSTOM) {
                                onOpenCustomizer()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Scroll wheel to browse • Center button to apply",
                color = palette.itemSubtitle,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    model: ThemeCardModel,
    isHighlighted: Boolean,
    isSelected: Boolean,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    val cardBackground = if (isHighlighted) palette.selectionGradient else palette.titleBarBrush
    val cardBorder = if (isHighlighted) palette.settingsItemValue else palette.screenBorder

    Column(
        modifier = Modifier
            .width(112.dp)
            .height(154.dp)
            .shadow(if (isHighlighted) 6.dp else 2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackground)
            .border(if (isHighlighted) 2.dp else 1.dp, cardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skeuomorphic miniature iPod chassis swatch
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(model.chassisColor)
                .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mini LCD Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (model.isScreenDark) Color(0xFF141418) else Color(0xFFF2F2F7))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                )

                // Mini Click Wheel
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(model.wheelColor)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(model.wheelColor.copy(alpha = 0.85f))
                            .border(0.5.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                    )
                }
            }
        }

        // Title and description
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = model.title,
                color = if (isHighlighted) palette.selectionText else palette.itemText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.subtitle,
                color = if (isHighlighted) palette.selectionText.copy(alpha = 0.8f) else palette.itemSubtitle,
                fontSize = 7.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action badge
        val badgeBg = when {
            isSelected -> Color(0xFF34C759)
            isHighlighted -> Color.White.copy(alpha = 0.25f)
            else -> palette.badgeBackground
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Active",
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = if (isSelected) "ACTIVE" else "SELECT",
                color = if (isSelected) Color.White else if (isHighlighted) palette.selectionText else palette.settingsItemValue,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


package com.retro.ipod.ui.sticker

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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.sticker.IpodSticker
import com.retro.ipod.data.sticker.StickerPresets
import com.retro.ipod.ui.theme.IpodScreenPalette
import com.retro.ipod.ui.wheel.IpodHapticFeedback

/**
 * Modern in-screen horizontal sticker card carousel.
 * Eliminates generic modal dialogs by embedding sticker selection directly inside the iPod LCD display.
 */
@Composable
fun PresetStickersPickerScreen(
    activeStickers: List<IpodSticker>,
    highlightedIndex: Int,
    palette: IpodScreenPalette,
    onSelectPreset: (StickerPresets.PresetMeta) -> Unit,
    onPickCustomPhoto: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = remember(context) { IpodHapticFeedback(context) }
    val presets = remember(context) { StickerPresets.getAllPresets(context) }
    val lazyRowState = rememberLazyListState()

    // Keep highlighted index in view when wheel rotates
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex in 0..presets.size) {
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
        // Title Bar
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
                        text = "Sticker Catalog",
                        color = palette.titleBarText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Slot Badge
                val isMaxReached = activeStickers.size >= 3
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isMaxReached) Color(0xFFC62828).copy(alpha = 0.2f)
                            else palette.badgeBackground
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${activeStickers.size}/3 Active",
                        color = if (isMaxReached) Color(0xFFE53935) else palette.settingsItemValue,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.dividerColor)
        )

        // Main Carousel Area
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
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First Card: Add Custom Photo / Decal
                item {
                    val isHighlighted = highlightedIndex == 0
                    CustomPhotoCard(
                        isHighlighted = isHighlighted,
                        isMaxReached = activeStickers.size >= 3,
                        palette = palette,
                        onClick = {
                            haptic.tick()
                            onPickCustomPhoto()
                        }
                    )
                }

                // Preset Cards
                itemsIndexed(presets, key = { _, meta -> meta.id }) { index, meta ->
                    val actualIndex = index + 1 // Account for custom photo card at index 0
                    val isHighlighted = highlightedIndex == actualIndex
                    val isEquipped = activeStickers.any { it.presetId == meta.id }

                    StickerCardItem(
                        meta = meta,
                        isHighlighted = isHighlighted,
                        isEquipped = isEquipped,
                        isMaxReached = activeStickers.size >= 3,
                        palette = palette,
                        onClick = {
                            haptic.tick()
                            onSelectPreset(meta)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Carousel instruction tip
            Text(
                text = "Scroll wheel to browse • Center button to equip",
                color = palette.itemSubtitle,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomPhotoCard(
    isHighlighted: Boolean,
    isMaxReached: Boolean,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    val cardBackground = if (isHighlighted) palette.selectionGradient else palette.titleBarBrush
    val cardBorder = if (isHighlighted) palette.settingsItemValue else palette.screenBorder

    Column(
        modifier = Modifier
            .width(108.dp)
            .height(148.dp)
            .shadow(if (isHighlighted) 6.dp else 2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackground)
            .border(if (isHighlighted) 2.dp else 1.dp, cardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(palette.badgeBackground.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = "Upload Custom Sticker",
                tint = if (isHighlighted) palette.selectionText else palette.settingsItemValue,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Custom Decal",
                color = if (isHighlighted) palette.selectionText else palette.itemText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "From Photos",
                color = if (isHighlighted) palette.selectionText.copy(alpha = 0.8f) else palette.itemSubtitle,
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isHighlighted) Color.White.copy(alpha = 0.25f) else palette.badgeBackground)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isMaxReached) "MAX 3/3" else "+ IMPORT",
                color = if (isHighlighted) palette.selectionText else palette.settingsItemValue,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StickerCardItem(
    meta: StickerPresets.PresetMeta,
    isHighlighted: Boolean,
    isEquipped: Boolean,
    isMaxReached: Boolean,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    val cardBackground = if (isHighlighted) palette.selectionGradient else palette.titleBarBrush
    val cardBorder = if (isHighlighted) palette.settingsItemValue else palette.screenBorder

    Column(
        modifier = Modifier
            .width(108.dp)
            .height(148.dp)
            .shadow(if (isHighlighted) 6.dp else 2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackground)
            .border(if (isHighlighted) 2.dp else 1.dp, cardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Graphic Preview
        Box(
            modifier = Modifier
                .size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            StickerGraphic(
                sticker = IpodSticker(
                    id = "preview_${meta.id}",
                    type = meta.type,
                    presetId = meta.id,
                    assetPath = meta.assetPath
                ),
                baseSize = 54.dp
            )
        }

        // Title and Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = meta.title,
                color = if (isHighlighted) palette.selectionText else palette.itemText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = meta.subtitle,
                color = if (isHighlighted) palette.selectionText.copy(alpha = 0.8f) else palette.itemSubtitle,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Action Pill
        val pillBg = when {
            isEquipped -> Color(0xFF34C759)
            isHighlighted -> Color.White.copy(alpha = 0.25f)
            else -> palette.badgeBackground
        }
        val pillText = when {
            isEquipped -> "EQUIPPED"
            isMaxReached -> "MAX 3/3"
            else -> "ADD +"
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(pillBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEquipped) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Equipped",
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = pillText,
                color = if (isEquipped) Color.White else if (isHighlighted) palette.selectionText else palette.settingsItemValue,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


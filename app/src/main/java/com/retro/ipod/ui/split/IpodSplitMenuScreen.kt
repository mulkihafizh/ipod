package com.retro.ipod.ui.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.ui.art.AlbumArtView
import com.retro.ipod.ui.navigation.IpodMenuItem
import com.retro.ipod.ui.theme.AppFonts
import com.retro.ipod.ui.theme.IpodScreenPalette

/**
 * Authentic 5th/6th Gen iPod Classic Split-Screen Menu Composable.
 *
 * Left Pane (~52%):
 * - Header with Menu Title
 * - Scrollable item list with signature blue gradient selection bar
 * - Right chevrons
 *
 * Right Pane (~48%):
 * - Contextual dynamic artwork preview of the currently highlighted item with floor reflection
 */
@Composable
fun IpodSplitMenuScreen(
    title: String,
    items: List<IpodMenuItem>,
    highlightedIndex: Int,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE),
    previewFilePath: String? = null,
    previewTitle: String? = null,
    previewArtist: String? = null,
    onItemClick: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.screenBackground)
            .border(1.dp, palette.screenBorder, RoundedCornerShape(8.dp))
    ) {
        // Left Column (List) - 54% width
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.54f)
                .background(palette.menuLeftBackground)
        ) {
            // Menu Title Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(palette.titleBarBrush)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    color = palette.titleBarText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.dividerColor)
            )

            // Items List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items",
                        color = palette.itemSubtitle,
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val isHighlighted = index == highlightedIndex

                        IpodSplitMenuItem(
                            item = item,
                            isHighlighted = isHighlighted,
                            palette = palette,
                            onClick = { onItemClick(index) }
                        )
                    }
                }
            }
        }

        // Center LCD Divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(palette.dividerColor)
        )

        // Right Column (Dynamic Contextual Preview) - 46% width
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.46f)
                .background(palette.menuRightBackground)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val currentItem = items.getOrNull(highlightedIndex)
            val effectivePath = previewFilePath ?: currentItem?.artworkSongPath
            val effectiveTitle = previewTitle ?: currentItem?.title
            val effectiveArtist = previewArtist ?: currentItem?.subtitle

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AlbumArtView(
                    filePath = effectivePath,
                    title = effectiveTitle,
                    artist = effectiveArtist,
                    size = 100.dp,
                    reflectionHeight = 26.dp,
                    withReflection = true,
                    cornerRadius = 4.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (effectiveTitle != null) {
                    Text(
                        text = effectiveTitle,
                        color = palette.itemText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFonts.helvetica(FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (effectiveArtist != null) {
                    Text(
                        text = effectiveArtist,
                        color = palette.itemSubtitle,
                        fontSize = 9.sp,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Single item row with iPod blue selection gradient.
 */
@Composable
fun IpodSplitMenuItem(
    item: IpodMenuItem,
    isHighlighted: Boolean,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE),
    onClick: () -> Unit
) {
    val backgroundBrush = if (isHighlighted) {
        palette.selectionGradient
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(backgroundBrush)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = if (isHighlighted) palette.selectionText else palette.itemText,
                fontSize = 11.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                fontFamily = AppFonts.helvetica(if (isHighlighted) FontWeight.Bold else FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.subtitle != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${item.subtitle})",
                    color = if (isHighlighted) palette.selectionText.copy(alpha = 0.85f) else palette.itemSubtitle,
                    fontSize = 9.sp,
                    fontFamily = AppFonts.helvetica(FontWeight.Normal),
                    maxLines = 1
                )
            }
        }

        if (item.hasSubMenu) {
            Text(
                text = "›",
                color = if (isHighlighted) palette.selectionText else palette.chevronColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

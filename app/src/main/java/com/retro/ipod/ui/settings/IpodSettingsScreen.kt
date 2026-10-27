package com.retro.ipod.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.settings.AppSettings
import com.retro.ipod.data.settings.BgMode
import com.retro.ipod.data.settings.LyricsDisplayMode
import com.retro.ipod.data.settings.SkinTheme

import com.retro.ipod.ui.theme.IpodScreenPalette

data class SettingsRowItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val valueLabel: String? = null,
    val isToggle: Boolean = false,
    val toggleValue: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Settings Sub-Menu View for iPod Classic.
 */
@Composable
fun IpodSettingsListScreen(
    title: String,
    items: List<SettingsRowItem>,
    highlightedIndex: Int,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE)
) {
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
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = palette.titleBarText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.dividerColor)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.menuLeftBackground)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val isHighlighted = index == highlightedIndex

                val backgroundBrush = if (isHighlighted) {
                    palette.selectionGradient
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(backgroundBrush)
                        .clickable { item.onClick() }
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = if (isHighlighted) palette.selectionText else palette.settingsItemText,
                            fontSize = 12.sp,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle,
                                color = if (isHighlighted) palette.selectionText.copy(alpha = 0.8f) else palette.itemSubtitle,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (item.isToggle) {
                        Switch(
                            checked = item.toggleValue,
                            onCheckedChange = { item.onClick() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4FC3F7),
                                checkedTrackColor = Color(0xFF0288D1).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    } else if (item.valueLabel != null) {
                        Text(
                            text = item.valueLabel,
                            color = if (isHighlighted) palette.selectionText else palette.settingsItemValue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "›",
                            color = if (isHighlighted) palette.selectionText else palette.chevronColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

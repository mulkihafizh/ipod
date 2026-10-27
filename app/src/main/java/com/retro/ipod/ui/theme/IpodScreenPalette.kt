package com.retro.ipod.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.retro.ipod.data.settings.SkinTheme

/**
 * Skeuomorphic and LCD screen palette mapping for iPod Classic hardware themes.
 * Adapts screen colors (light/dark LCD, ruby accents, graphite) to match the selected SkinTheme.
 */
data class IpodScreenPalette(
    val theme: SkinTheme,
    // Outer display frame & bevel
    val bezelBackground: Color,
    val screenBackground: Color,
    val screenBorder: Color,
    // Mini header status bar
    val headerBarBackground: Color,
    val headerBarBorder: Color,
    val headerBarText: Color,
    val headerBarMuted: Color,
    val batteryBorder: Color,
    val batteryFill: Color,
    // Split menu panes
    val menuLeftBackground: Color,
    val menuRightBackground: Brush,
    val titleBarBrush: Brush,
    val titleBarText: Color,
    val dividerColor: Color,
    val itemText: Color,
    val itemSubtitle: Color,
    val chevronColor: Color,
    val selectionGradient: Brush,
    val selectionText: Color,
    // Now Playing view
    val nowPlayingBackground: Color,
    val nowPlayingBorder: Color,
    val nowPlayingTextPrimary: Color,
    val nowPlayingTextSecondary: Color,
    val nowPlayingTextTertiary: Color,
    val scrubberTrack: Color,
    val scrubberBorder: Color,
    val scrubberProgress: Brush,
    val scrubberThumb: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val starFilled: Color,
    val starEmpty: Color,
    // Settings list
    val settingsItemText: Color,
    val settingsItemValue: Color
) {
    companion object {
        fun forTheme(theme: SkinTheme): IpodScreenPalette = when (theme) {
            SkinTheme.CLASSIC_WHITE -> IpodScreenPalette(
                theme = theme,
                bezelBackground = Color(0xFFD6D6DE),
                screenBackground = Color(0xFFF2F2F7),
                screenBorder = Color(0xFFC7C7CC),
                headerBarBackground = Color(0xFFE5E5EA),
                headerBarBorder = Color(0xFFD1D1D6),
                headerBarText = Color(0xFF1C1C1E),
                headerBarMuted = Color(0xFF636366),
                batteryBorder = Color(0xFF34C759),
                batteryFill = Color(0xFF34C759),
                menuLeftBackground = Color(0xFFF7F7FA),
                menuRightBackground = Brush.verticalGradient(
                    listOf(Color(0xFFEFEFF4), Color(0xFFDFDFE6))
                ),
                titleBarBrush = Brush.verticalGradient(
                    listOf(Color(0xFFE8E8EE), Color(0xFFD2D2DA))
                ),
                titleBarText = Color(0xFF1C1C1E),
                dividerColor = Color(0xFFC8C8CC),
                itemText = Color(0xFF1C1C1E),
                itemSubtitle = Color(0xFF6C6C70),
                chevronColor = Color(0xFF8E8E93),
                selectionGradient = Brush.verticalGradient(
                    listOf(Color(0xFF0288D1), Color(0xFF01579B))
                ),
                selectionText = Color.White,
                nowPlayingBackground = Color(0xFFF2F2F7),
                nowPlayingBorder = Color(0xFFC7C7CC),
                nowPlayingTextPrimary = Color(0xFF1C1C1E),
                nowPlayingTextSecondary = Color(0xFF3A3A3C),
                nowPlayingTextTertiary = Color(0xFF636366),
                scrubberTrack = Color(0xFFD8D8DE),
                scrubberBorder = Color(0xFFC7C7CC),
                scrubberProgress = Brush.horizontalGradient(
                    listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                ),
                scrubberThumb = Color(0xFF0288D1),
                badgeBackground = Color(0xFFE0E0E6),
                badgeText = Color(0xFF1C1C1E),
                starFilled = Color(0xFFFFB300),
                starEmpty = Color(0xFFB0B0B8),
                settingsItemText = Color(0xFF1C1C1E),
                settingsItemValue = Color(0xFF0288D1)
            )

            SkinTheme.U2_EDITION -> IpodScreenPalette(
                theme = theme,
                bezelBackground = Color(0xFF060608),
                screenBackground = Color(0xFF0C0C0E),
                screenBorder = Color(0xFF3A0E0E),
                headerBarBackground = Color(0xFF140404),
                headerBarBorder = Color(0xFF3A0808),
                headerBarText = Color.White,
                headerBarMuted = Color(0xFFFF5252),
                batteryBorder = Color(0xFFFF5252),
                batteryFill = Color(0xFFFF5252),
                menuLeftBackground = Color(0xFF101014),
                menuRightBackground = Brush.radialGradient(
                    listOf(Color(0xFF280606), Color(0xFF0C0C0E))
                ),
                titleBarBrush = Brush.verticalGradient(
                    listOf(Color(0xFF7A0E0E), Color(0xFF3E0505))
                ),
                titleBarText = Color.White,
                dividerColor = Color(0xFF4A0E0E),
                itemText = Color(0xFFEEEEEE),
                itemSubtitle = Color(0xFFFF8A80),
                chevronColor = Color(0xFFFF5252),
                selectionGradient = Brush.verticalGradient(
                    listOf(Color(0xFFD32F2F), Color(0xFF8B0000))
                ),
                selectionText = Color.White,
                nowPlayingBackground = Color(0xFF0E0E12),
                nowPlayingBorder = Color(0xFF3A0E0E),
                nowPlayingTextPrimary = Color.White,
                nowPlayingTextSecondary = Color(0xFFFF8A80),
                nowPlayingTextTertiary = Color(0xFFB0BEC5),
                scrubberTrack = Color(0xFF241010),
                scrubberBorder = Color(0xFF3A0E0E),
                scrubberProgress = Brush.horizontalGradient(
                    listOf(Color(0xFFFF5252), Color(0xFFD50000))
                ),
                scrubberThumb = Color(0xFFFF5252),
                badgeBackground = Color(0xFF2A0808),
                badgeText = Color(0xFFFF8A80),
                starFilled = Color(0xFFFF5252),
                starEmpty = Color(0xFF4A1A1A),
                settingsItemText = Color.White,
                settingsItemValue = Color(0xFFFF5252)
            )

            SkinTheme.BRUSHED_ALUMINUM -> IpodScreenPalette(
                theme = theme,
                bezelBackground = Color(0xFF101014),
                screenBackground = Color(0xFF16161C),
                screenBorder = Color(0xFF282834),
                headerBarBackground = Color(0xFF14141A),
                headerBarBorder = Color(0xFF282834),
                headerBarText = Color.White,
                headerBarMuted = Color(0xFF8E8E93),
                batteryBorder = Color(0xFF81C784),
                batteryFill = Color(0xFF81C784),
                menuLeftBackground = Color(0xFF181820),
                menuRightBackground = Brush.radialGradient(
                    listOf(Color(0xFF22222C), Color(0xFF14141A))
                ),
                titleBarBrush = Brush.verticalGradient(
                    listOf(Color(0xFF2E2E38), Color(0xFF1C1C24))
                ),
                titleBarText = Color.White,
                dividerColor = Color(0xFF383844),
                itemText = Color(0xFFE0E0E0),
                itemSubtitle = Color(0xFF757575),
                chevronColor = Color(0xFF757575),
                selectionGradient = Brush.verticalGradient(
                    listOf(Color(0xFF0288D1), Color(0xFF01579B))
                ),
                selectionText = Color.White,
                nowPlayingBackground = Color(0xFF16161E),
                nowPlayingBorder = Color(0xFF282834),
                nowPlayingTextPrimary = Color.White,
                nowPlayingTextSecondary = Color(0xFFB0BEC5),
                nowPlayingTextTertiary = Color(0xFF757575),
                scrubberTrack = Color(0xFF22222E),
                scrubberBorder = Color(0xFF14141C),
                scrubberProgress = Brush.horizontalGradient(
                    listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                ),
                scrubberThumb = Color.White,
                badgeBackground = Color(0xFF242430),
                badgeText = Color(0xFFB0BEC5),
                starFilled = Color(0xFFFFD54F),
                starEmpty = Color(0xFF42424E),
                settingsItemText = Color.White,
                settingsItemValue = Color(0xFF4FC3F7)
            )
        }
    }
}

package com.retro.ipod.ui.navigation

/**
 * Representation of each hierarchical screen in the iPod classic UI stack.
 */
sealed interface IpodScreen {
    object MainMenu : IpodScreen
    object MusicMenu : IpodScreen
    object PlaylistsList : IpodScreen
    data class PlaylistDetail(val playlistId: Long, val name: String) : IpodScreen
    data class AddSongsToPlaylist(val playlistId: Long, val name: String) : IpodScreen
    object ArtistsList : IpodScreen
    data class ArtistAlbums(val artist: String) : IpodScreen
    object AlbumsList : IpodScreen
    data class AlbumSongs(val album: String, val artist: String? = null) : IpodScreen
    object AllSongsList : IpodScreen
    object SearchScreen : IpodScreen
    object NowPlayingScreen : IpodScreen
    object LyricsScreen : IpodScreen
    object SettingsMenu : IpodScreen
    object AudioSettings : IpodScreen
    object LyricsSettings : IpodScreen
    object ThemeSettings : IpodScreen
    object StickerSettings : IpodScreen
}

/**
 * Navigation item representation for the split menu list.
 */
data class IpodMenuItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val hasSubMenu: Boolean = true,
    val artworkSongPath: String? = null,
    val onClick: () -> Unit
)

/**
 * Stateful navigation controller for the iPod hierarchical stack.
 */
class IpodNavController(
    initialScreen: IpodScreen = IpodScreen.MainMenu
) {
    private val backStack = mutableListOf<IpodScreen>()
    var currentScreen: IpodScreen = initialScreen
        private set

    // Memory of highlighted indexes for each screen so back navigation restores cursor position
    private val highlightIndexMap = mutableMapOf<String, Int>()

    fun push(screen: IpodScreen) {
        if (screen != currentScreen) {
            backStack.add(currentScreen)
            currentScreen = screen
        }
    }

    /**
     * Pops the top screen from the back stack.
     * Returns true if successfully popped, false if already at root (MainMenu).
     */
    fun pop(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
            true
        } else {
            false
        }
    }

    fun jumpToNowPlaying() {
        if (currentScreen != IpodScreen.NowPlayingScreen) {
            backStack.add(currentScreen)
            currentScreen = IpodScreen.NowPlayingScreen
        }
    }

    fun jumpToLyrics() {
        if (currentScreen != IpodScreen.LyricsScreen) {
            backStack.add(currentScreen)
            currentScreen = IpodScreen.LyricsScreen
        }
    }

    fun getHighlightedIndex(screenKey: String): Int {
        return highlightIndexMap[screenKey] ?: 0
    }

    fun setHighlightedIndex(screenKey: String, index: Int) {
        highlightIndexMap[screenKey] = index
    }

    val canGoBack: Boolean
        get() = backStack.isNotEmpty()
}

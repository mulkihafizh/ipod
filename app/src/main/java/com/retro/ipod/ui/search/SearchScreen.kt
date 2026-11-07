package com.retro.ipod.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.db.entity.SongEntity
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.ui.art.AlbumArtView
import com.retro.ipod.ui.theme.AppFonts
import com.retro.ipod.ui.theme.IpodScreenPalette

/**
 * Intelligent Music Search Screen adhering to Section 3.4 of the iPod UI/UX Specification.
 * Features Scope Bar Filter Chips, Top Result Best Match Card, and Sectioned Breakdown.
 */
@Composable
fun SearchScreen(
    allSongs: List<SongEntity>,
    onSongSelected: (SongEntity, List<SongEntity>) -> Unit,
    onAlbumSelected: (String, String?) -> Unit,
    onArtistSelected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    palette: IpodScreenPalette = IpodScreenPalette.forTheme(SkinTheme.CLASSIC_WHITE)
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf(SearchScope.ALL) }
    val focusManager = LocalFocusManager.current

    val searchResult = remember(searchQuery, selectedScope, allSongs) {
        SearchRankingEngine.executeSearch(searchQuery, allSongs, selectedScope)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.screenBackground)
            .border(1.dp, palette.screenBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Search Header Bar + Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.headerBarBackground)
                .border(1.dp, palette.headerBarBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = palette.headerBarMuted,
                modifier = Modifier
                    .size(17.dp)
                    .padding(end = 4.dp)
            )

            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = TextStyle(
                    color = palette.itemText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(palette.settingsItemValue),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search songs, artists, albums...",
                                color = palette.itemSubtitle,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = palette.headerBarMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { searchQuery = "" }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scope Filter Bar Chips (Spec 4.1)
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SearchScope.values().forEach { scope ->
                val isSelected = scope == selectedScope
                val chipBackground = if (isSelected) {
                    palette.selectionGradient
                } else {
                    Brush.linearGradient(listOf(palette.badgeBackground, palette.badgeBackground))
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBackground)
                        .clickable { selectedScope = scope }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scope.label,
                        color = if (isSelected) palette.selectionText else palette.itemSubtitle,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Results Content
        if (searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Music Search",
                        color = palette.headerBarMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Quickly find songs, albums, and artists in your audio library.",
                        color = palette.itemSubtitle,
                        fontSize = 10.5.sp,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            val hasResults = searchResult.topResult != null ||
                searchResult.matchingSongs.isNotEmpty() ||
                searchResult.matchingAlbums.isNotEmpty() ||
                searchResult.matchingArtists.isNotEmpty()

            if (!hasResults) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "No Results Found",
                            color = palette.headerBarMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "No matches found for \"$searchQuery\". Try checking the spelling or searching another artist or title.",
                            color = palette.itemSubtitle,
                            fontSize = 10.sp,
                            fontFamily = AppFonts.helvetica(FontWeight.Normal),
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // 1. Top Result Card (Spec 4.2.3)
                val top = searchResult.topResult
                if (top != null) {
                    item {
                        Text(
                            text = "TOP RESULT",
                            color = palette.settingsItemValue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        TopResultCard(
                            topResult = top,
                            palette = palette,
                            onPlayTrack = { song -> onSongSelected(song, allSongs) },
                            onSelectArtist = { artist -> onArtistSelected(artist) },
                            onSelectAlbum = { album, artist -> onAlbumSelected(album, artist) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // 2. Songs Section
                if (searchResult.matchingSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "SONGS (${searchResult.matchingSongs.size})",
                            color = palette.headerBarMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingSongs.take(15), key = { it.id }) { song ->
                        SearchSongItem(
                            song = song,
                            palette = palette,
                            onClick = { onSongSelected(song, searchResult.matchingSongs) }
                        )
                    }
                }

                // 3. Albums Section
                if (searchResult.matchingAlbums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ALBUMS (${searchResult.matchingAlbums.size})",
                            color = palette.headerBarMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingAlbums.take(8), key = { it.album + it.artist }) { albumGroup ->
                        SearchAlbumItem(
                            albumGroup = albumGroup,
                            palette = palette,
                            onClick = { onAlbumSelected(albumGroup.album, albumGroup.artist) }
                        )
                    }
                }

                // 4. Artists Section
                if (searchResult.matchingArtists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ARTISTS (${searchResult.matchingArtists.size})",
                            color = palette.headerBarMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingArtists.take(8), key = { it.artist }) { artistGroup ->
                        SearchArtistItem(
                            artistGroup = artistGroup,
                            palette = palette,
                            onClick = { onArtistSelected(artistGroup.artist) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun TopResultCard(
    topResult: TopResult,
    palette: IpodScreenPalette,
    onPlayTrack: (SongEntity) -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String, String) -> Unit
) {
    val (title, subtitle, badgeText, artworkPath) = when (topResult) {
        is TopResult.ArtistResult -> {
            Tuple4(
                topResult.artist,
                "${topResult.trackCount} songs in library",
                "ARTIST",
                topResult.sampleSong?.filePath
            )
        }
        is TopResult.TrackResult -> {
            Tuple4(
                topResult.song.title,
                "${topResult.song.artist} • ${topResult.song.album}",
                "TRACK",
                topResult.song.filePath
            )
        }
        is TopResult.AlbumResult -> {
            Tuple4(
                topResult.album,
                "${topResult.artist} • ${topResult.trackCount} songs",
                "ALBUM",
                topResult.sampleSong?.filePath
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(palette.badgeBackground, palette.menuLeftBackground)
                )
            )
            .border(1.dp, palette.headerBarBorder, RoundedCornerShape(8.dp))
            .clickable {
                when (topResult) {
                    is TopResult.TrackResult -> onPlayTrack(topResult.song)
                    is TopResult.ArtistResult -> onSelectArtist(topResult.artist)
                    is TopResult.AlbumResult -> onSelectAlbum(topResult.album, topResult.artist)
                }
            }
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtView(
                filePath = artworkPath,
                title = title,
                artist = subtitle,
                size = 44.dp,
                withReflection = false,
                cornerRadius = 4.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = palette.itemText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(palette.selectionGradient, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = palette.selectionText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = subtitle,
                    color = palette.itemSubtitle,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(palette.selectionGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = palette.selectionText,
                modifier = Modifier
                    .size(18.dp)
                    .offset(x = 1.dp)
            )
        }
    }
}

@Composable
fun SearchSongItem(
    song: SongEntity,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.menuLeftBackground)
            .border(0.5.dp, palette.dividerColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play track",
                    tint = palette.settingsItemValue,
                    modifier = Modifier
                        .size(13.dp)
                        .offset(x = 0.8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = song.title,
                    color = palette.itemText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = palette.itemSubtitle,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .background(palette.badgeBackground, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "${song.codec} ${song.sampleRateKhz}k",
                color = palette.badgeText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchAlbumItem(
    albumGroup: AlbumGroup,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.menuLeftBackground)
            .border(0.5.dp, palette.dividerColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArtView(
                filePath = albumGroup.songs.firstOrNull()?.filePath,
                title = albumGroup.album,
                artist = albumGroup.artist,
                size = 28.dp,
                withReflection = false,
                cornerRadius = 3.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = albumGroup.album,
                    color = palette.itemText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${albumGroup.artist} • ${albumGroup.songs.size} tracks",
                    color = palette.itemSubtitle,
                    fontSize = 9.sp
                )
            }
        }

        Text(text = "›", color = palette.chevronColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchArtistItem(
    artistGroup: ArtistGroup,
    palette: IpodScreenPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.menuLeftBackground)
            .border(0.5.dp, palette.dividerColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                .clip(CircleShape)
                .background(palette.selectionGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artistGroup.artist.take(1).uppercase(),
                    color = palette.selectionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = artistGroup.artist,
                    color = palette.itemText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${artistGroup.songCount} songs in library",
                    color = palette.itemSubtitle,
                    fontSize = 9.sp
                )
            }
        }

        Text(text = "›", color = palette.chevronColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

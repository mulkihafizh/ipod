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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.db.entity.SongEntity
import com.retro.ipod.ui.art.AlbumArtView
import com.retro.ipod.ui.theme.AppFonts

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
    modifier: Modifier = Modifier
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
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF282832), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Search Header Bar + Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202028))
                .border(1.dp, Color(0xFF333342), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = Color(0xFF8E8E93),
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )

            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF4FC3F7)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search tracks, albums, artists...",
                            color = Color(0xFF757575),
                            fontSize = 11.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = Color(0xFFB0BEC5),
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
                    Brush.verticalGradient(listOf(Color(0xFF0288D1), Color(0xFF01579B)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF252530), Color(0xFF252530)))
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
                        color = if (isSelected) Color.White else Color(0xFFA0A0A0),
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "iPod Intelligent Search",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Exact artist match prioritizes artist card.\nTracks & albums ranked by relevancy.",
                        color = Color(0xFF555560),
                        fontSize = 10.sp,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal),
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
                            color = Color(0xFF4FC3F7),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        TopResultCard(
                            topResult = top,
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
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingSongs.take(15), key = { it.id }) { song ->
                        SearchSongItem(
                            song = song,
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
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingAlbums.take(8), key = { it.album + it.artist }) { albumGroup ->
                        SearchAlbumItem(
                            albumGroup = albumGroup,
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
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResult.matchingArtists.take(8), key = { it.artist }) { artistGroup ->
                        SearchArtistItem(
                            artistGroup = artistGroup,
                            onClick = { onArtistSelected(artistGroup.artist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopResultCard(
    topResult: TopResult,
    onPlayTrack: (SongEntity) -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String, String) -> Unit
) {
    val (title, subtitle, badgeText, artworkPath) = when (topResult) {
        is TopResult.ArtistResult -> {
            Tuple4(
                topResult.artist,
                "${topResult.trackCount} Tracks in Library",
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
                "${topResult.artist} • ${topResult.trackCount} Tracks",
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
                    listOf(Color(0xFF0288D1).copy(alpha = 0.25f), Color(0xFF1E1E26))
                )
            )
            .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0288D1), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = subtitle,
                    color = Color(0xFFB0BEC5),
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
                .background(Color(0xFF0288D1)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "▶", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun SearchSongItem(
    song: SongEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1B1B22))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "▶ ", color = Color(0xFF4FC3F7), fontSize = 10.sp)
            Column {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = Color(0xFF8E8E93),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .background(Color(0xFF282834), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "${song.codec} ${song.sampleRateKhz}k",
                color = Color(0xFFFFD54F),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchAlbumItem(
    albumGroup: AlbumGroup,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1B1B22))
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
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${albumGroup.artist} • ${albumGroup.songs.size} tracks",
                    color = Color(0xFF8E8E93),
                    fontSize = 9.sp
                )
            }
        }

        Text(text = "›", color = Color(0xFF757575), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchArtistItem(
    artistGroup: ArtistGroup,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1B1B22))
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
                    .background(Color(0xFF0288D1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artistGroup.artist.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = artistGroup.artist,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${artistGroup.songCount} Songs in Library",
                    color = Color(0xFF8E8E93),
                    fontSize = 9.sp
                )
            }
        }

        Text(text = "›", color = Color(0xFF757575), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

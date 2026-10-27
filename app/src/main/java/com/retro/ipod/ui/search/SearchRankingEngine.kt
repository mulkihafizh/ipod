package com.retro.ipod.ui.search

import com.retro.ipod.data.db.entity.SongEntity
import kotlin.math.max
import kotlin.math.min

/**
 * Filter scope for search queries.
 */
enum class SearchScope(val label: String) {
    ALL("All"),
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

/**
 * Top Result item model.
 */
sealed class TopResult {
    data class ArtistResult(val artist: String, val trackCount: Int, val sampleSong: SongEntity?) : TopResult()
    data class TrackResult(val song: SongEntity) : TopResult()
    data class AlbumResult(val album: String, val artist: String, val trackCount: Int, val sampleSong: SongEntity?) : TopResult()
}

/**
 * Complete evaluated search result containing Top Result and sectioned breakdown.
 */
data class SearchResult(
    val query: String,
    val topResult: TopResult?,
    val matchingSongs: List<SongEntity>,
    val matchingAlbums: List<AlbumGroup>,
    val matchingArtists: List<ArtistGroup>
)

data class AlbumGroup(
    val album: String,
    val artist: String,
    val songs: List<SongEntity>
)

data class ArtistGroup(
    val artist: String,
    val songCount: Int,
    val sampleSong: SongEntity?
)

/**
 * Intelligent weighted search ranking engine implementing commercial streaming heuristics.
 */
object SearchRankingEngine {

    fun executeSearch(
        query: String,
        allSongs: List<SongEntity>,
        scope: SearchScope = SearchScope.ALL
    ): SearchResult {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) {
            return SearchResult(
                query = query,
                topResult = null,
                matchingSongs = emptyList(),
                matchingAlbums = emptyList(),
                matchingArtists = emptyList()
            )
        }

        // Group library by Artist and Album
        val artistMap = allSongs.groupBy { it.artist }
        val albumMap = allSongs.groupBy { "${it.album}:::${it.artist}" }

        // Find exact or highly similar artist matches (>= 95% Levenshtein)
        var exactArtistMatch: String? = null
        for (artistName in artistMap.keys) {
            val lowerArtist = artistName.lowercase()
            if (lowerArtist == trimmed) {
                exactArtistMatch = artistName
                break
            } else {
                val similarity = calculateSimilarity(lowerArtist, trimmed)
                if (similarity >= 0.95) {
                    exactArtistMatch = artistName
                    break
                }
            }
        }

        // 1. Exact Artist Overwrite Rule (Spec 4.2.1):
        // If query matches an Artist name exactly, prioritize that Artist Card as Top Result.
        var topResult: TopResult? = null
        if (exactArtistMatch != null && (scope == SearchScope.ALL || scope == SearchScope.ARTISTS)) {
            val songs = artistMap[exactArtistMatch].orEmpty()
            topResult = TopResult.ArtistResult(
                artist = exactArtistMatch,
                trackCount = songs.size,
                sampleSong = songs.firstOrNull()
            )
        }

        // Filter Songs
        val matchedSongs = if (scope == SearchScope.ALL || scope == SearchScope.TRACKS) {
            allSongs.filter { song ->
                song.title.lowercase().contains(trimmed) ||
                song.artist.lowercase().contains(trimmed) ||
                song.album.lowercase().contains(trimmed)
            }.sortedWith(compareBy(
                // Exact title match comes first
                { !(it.title.lowercase() == trimmed) },
                // Title starts with query
                { !(it.title.lowercase().startsWith(trimmed)) },
                // Artist starts with query
                { !(it.artist.lowercase().startsWith(trimmed)) },
                { it.title }
            ))
        } else emptyList()

        // Filter Albums
        val matchedAlbums = if (scope == SearchScope.ALL || scope == SearchScope.ALBUMS) {
            albumMap.entries.mapNotNull { (key, songs) ->
                val parts = key.split(":::")
                val albumName = parts.getOrNull(0) ?: ""
                val artistName = parts.getOrNull(1) ?: ""
                if (albumName.lowercase().contains(trimmed) || artistName.lowercase().contains(trimmed)) {
                    AlbumGroup(album = albumName, artist = artistName, songs = songs)
                } else null
            }.sortedWith(compareBy(
                { !(it.album.lowercase() == trimmed) },
                { !(it.album.lowercase().startsWith(trimmed)) },
                { it.album }
            ))
        } else emptyList()

        // Filter Artists
        val matchedArtists = if (scope == SearchScope.ALL || scope == SearchScope.ARTISTS) {
            artistMap.entries.mapNotNull { (artistName, songs) ->
                if (artistName.lowercase().contains(trimmed)) {
                    ArtistGroup(artist = artistName, songCount = songs.size, sampleSong = songs.firstOrNull())
                } else null
            }.sortedWith(compareBy(
                { !(it.artist.lowercase() == trimmed) },
                { !(it.artist.lowercase().startsWith(trimmed)) },
                { it.artist }
            ))
        } else emptyList()

        // 2. Track -> Album Priority Rule (Spec 4.2.2):
        // If not an exact artist match, prioritize matching Track -> Album -> Artist
        if (topResult == null) {
            val firstTrack = matchedSongs.firstOrNull()
            val firstAlbum = matchedAlbums.firstOrNull()
            val firstArtist = matchedArtists.firstOrNull()

            topResult = when {
                firstTrack != null && firstTrack.title.lowercase().contains(trimmed) -> {
                    TopResult.TrackResult(firstTrack)
                }
                firstAlbum != null && firstAlbum.album.lowercase().contains(trimmed) -> {
                    TopResult.AlbumResult(
                        album = firstAlbum.album,
                        artist = firstAlbum.artist,
                        trackCount = firstAlbum.songs.size,
                        sampleSong = firstAlbum.songs.firstOrNull()
                    )
                }
                firstArtist != null -> {
                    TopResult.ArtistResult(
                        artist = firstArtist.artist,
                        trackCount = firstArtist.songCount,
                        sampleSong = firstArtist.sampleSong
                    )
                }
                firstTrack != null -> TopResult.TrackResult(firstTrack)
                else -> null
            }
        }

        return SearchResult(
            query = query,
            topResult = topResult,
            matchingSongs = matchedSongs,
            matchingAlbums = matchedAlbums,
            matchingArtists = matchedArtists
        )
    }

    /**
     * Normalized Levenshtein distance similarity [0.0, 1.0].
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toDouble() / maxLen.toDouble()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}

package com.retro.ipod.data.playlist

import android.content.Context
import android.net.Uri
import android.util.Log
import com.retro.ipod.data.db.IpodDatabase
import com.retro.ipod.data.db.entity.PlaylistEntity
import com.retro.ipod.data.db.entity.PlaylistSongCrossRef
import com.retro.ipod.data.db.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

data class M3uImportResult(
    val playlistName: String,
    val totalTracksFound: Int,
    val totalEntriesParsed: Int,
    val success: Boolean,
    val message: String
)

/**
 * High-performance parser and importer for standard M3U and M3U8 audio playlist files.
 * Supports absolute paths, relative folder paths, URI resolutions, and filename matching.
 */
class M3uPlaylistManager(
    private val context: Context,
    private val database: IpodDatabase
) {
    companion object {
        private const val TAG = "M3uPlaylistManager"
    }

    suspend fun importM3uFromUri(uri: Uri, overrideName: String? = null): M3uImportResult = withContext(Dispatchers.IO) {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open M3U input stream from URI: $uri", e)
            null
        } ?: return@withContext M3uImportResult(
            playlistName = overrideName ?: "Imported Playlist",
            totalTracksFound = 0,
            totalEntriesParsed = 0,
            success = false,
            message = "Could not open selected playlist file."
        )

        val resolvedName = overrideName ?: resolveNameFromUri(uri)
        importM3uStream(inputStream, resolvedName)
    }

    suspend fun importM3uFromFile(file: File): M3uImportResult = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) {
            return@withContext M3uImportResult(
                playlistName = file.nameWithoutExtension,
                totalTracksFound = 0,
                totalEntriesParsed = 0,
                success = false,
                message = "File does not exist or cannot be read."
            )
        }
        val inputStream = file.inputStream()
        importM3uStream(inputStream, file.nameWithoutExtension)
    }

    private suspend fun importM3uStream(inputStream: InputStream, playlistName: String): M3uImportResult {
        val rawEntries = mutableListOf<String>()
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        rawEntries.add(trimmed)
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading M3U stream", e)
            return M3uImportResult(
                playlistName = playlistName,
                totalTracksFound = 0,
                totalEntriesParsed = 0,
                success = false,
                message = "Failed to parse M3U file format."
            )
        }

        if (rawEntries.isEmpty()) {
            return M3uImportResult(
                playlistName = playlistName,
                totalTracksFound = 0,
                totalEntriesParsed = 0,
                success = false,
                message = "Playlist file contains no valid audio entries."
            )
        }

        val allSongs = database.musicNavigationDao().getAllSongsSnapshot()
        val pathMap = allSongs.associateBy { it.filePath.replace('\\', '/').lowercase() }
        val nameMap = allSongs.groupBy { File(it.filePath).name.lowercase() }
        val titleMap = allSongs.groupBy { it.title.trim().lowercase() }

        val matchedSongs = mutableListOf<SongEntity>()

        for (entry in rawEntries) {
            val normEntry = entry.replace('\\', '/').trim()
            val entryFileName = File(normEntry).name.lowercase()
            val entryTitle = File(normEntry).nameWithoutExtension.lowercase()

            // 1. Exact or suffix path match
            val exactMatch = pathMap[normEntry.lowercase()]
                ?: allSongs.firstOrNull { it.filePath.replace('\\', '/').endsWith(normEntry, ignoreCase = true) }

            // 2. Exact filename match
            val fileMatch = exactMatch ?: nameMap[entryFileName]?.firstOrNull()

            // 3. Title match
            val titleMatch = fileMatch ?: titleMap[entryTitle]?.firstOrNull()

            if (titleMatch != null && !matchedSongs.contains(titleMatch)) {
                matchedSongs.add(titleMatch)
            }
        }

        if (matchedSongs.isEmpty()) {
            return M3uImportResult(
                playlistName = playlistName,
                totalTracksFound = 0,
                totalEntriesParsed = rawEntries.size,
                success = false,
                message = "None of the tracks in the M3U file were found in your library."
            )
        }

        val playlistDao = database.playlistDao()
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = playlistName,
                createdAt = System.currentTimeMillis()
            )
        )

        matchedSongs.forEachIndexed { index, song ->
            playlistDao.insertPlaylistSongCrossRef(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    id = song.id,
                    orderIndex = index
                )
            )
        }

        return M3uImportResult(
            playlistName = playlistName,
            totalTracksFound = matchedSongs.size,
            totalEntriesParsed = rawEntries.size,
            success = true,
            message = "Successfully imported '$playlistName' with ${matchedSongs.size} tracks."
        )
    }

    private fun resolveNameFromUri(uri: Uri): String {
        return try {
            val path = uri.path ?: return "Imported Playlist"
            val lastSegment = uri.lastPathSegment ?: path
            val fileName = File(lastSegment).name
            if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName.ifBlank { "Imported Playlist" }
        } catch (_: Exception) {
            "Imported Playlist"
        }
    }
}

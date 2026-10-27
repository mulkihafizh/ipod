package com.retro.ipod.lyrics.repository

import android.util.Log
import com.retro.ipod.data.db.entity.SongEntity
import com.retro.ipod.lyrics.model.LyricsDocument
import com.retro.ipod.lyrics.parser.EmbeddedLyricsExtractor
import com.retro.ipod.lyrics.parser.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository responsible for loading, resolving, and caching lyrics documents.
 * Priority hierarchy:
 * 1. Memory cache
 * 2. External sidecar `.lrc` file adjacent to audio file (<title>.lrc)
 * 3. External `.lrc` file in adjacent `lyrics/` or `Lyrics/` subfolder
 * 4. External sidecar `.txt` file (<title>.txt)
 * 5. Embedded lyrics inside the audio file (.flac Vorbis comment, .mp3 ID3 USLT/SYLT, .m4a ©lyr atom)
 * 6. Procedural demo lyrics fallback (ensuring the visualizer is always functional and interactive)
 */
class LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
    }

    private val memoryCache = mutableMapOf<Long, LyricsDocument>()

    suspend fun getLyricsForSong(song: SongEntity?): LyricsDocument = withContext(Dispatchers.IO) {
        if (song == null) return@withContext LyricsDocument()

        memoryCache[song.id]?.let { return@withContext it }

        // 1. Check external files (.lrc, .txt)
        val externalDoc = resolveFromDisk(song)

        // 2. Check embedded lyrics inside audio file (.flac, .mp3, .m4a)
        val embeddedDoc = if (externalDoc == null) {
            resolveFromEmbedded(song)
        } else null

        // 3. No template fallback: return empty lyrics document if neither external nor embedded lyrics exist
        val resolvedDoc = externalDoc ?: embeddedDoc ?: LyricsDocument(
            title = song.title,
            artist = song.artist,
            album = song.album,
            lines = emptyList()
        )

        memoryCache[song.id] = resolvedDoc
        return@withContext resolvedDoc
    }

    /**
     * Resolves external sidecar lyrics (.lrc or .txt) adjacent to the song file or in a lyrics subfolder.
     */
    private fun resolveFromDisk(song: SongEntity): LyricsDocument? {
        val path: String = song.filePath
        if (path.isBlank()) return null

        try {
            val audioFile = File(path)
            if (!audioFile.exists()) return null

            val songBaseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile

            // 1. Direct .lrc replacement
            val directLrcPath = if (path.contains('.')) {
                path.substringBeforeLast('.') + ".lrc"
            } else {
                "$path.lrc"
            }
            val directLrcFile = File(directLrcPath)
            if (directLrcFile.exists() && directLrcFile.canRead()) {
                val content = directLrcFile.readText()
                if (content.isNotBlank()) {
                    Log.d(TAG, "Loaded external sidecar .lrc for track: ${song.title}")
                    return LrcParser.parse(content, durationMs = song.durationMs)
                }
            }

            if (parent != null && parent.isDirectory) {
                // 2. Case-insensitive match in same parent directory
                val matchingLrc = parent.listFiles { file: File ->
                    file.isFile && file.nameWithoutExtension.equals(songBaseName, ignoreCase = true)
                            && file.extension.equals("lrc", ignoreCase = true)
                }?.firstOrNull()

                if (matchingLrc != null && matchingLrc.canRead()) {
                    val content = matchingLrc.readText()
                    if (content.isNotBlank()) {
                        Log.d(TAG, "Loaded case-insensitive .lrc for track: ${song.title}")
                        return LrcParser.parse(content, durationMs = song.durationMs)
                    }
                }

                // 3. Search in 'lyrics' or 'Lyrics' subfolder
                val subfolders = listOf("lyrics", "Lyrics", "LYRICS")
                for (sub in subfolders) {
                    val lyricsDir = File(parent, sub)
                    if (lyricsDir.isDirectory) {
                        val subLrc = lyricsDir.listFiles { file: File ->
                            file.isFile && file.nameWithoutExtension.equals(songBaseName, ignoreCase = true)
                                    && file.extension.equals("lrc", ignoreCase = true)
                        }?.firstOrNull()

                        if (subLrc != null && subLrc.canRead()) {
                            val content = subLrc.readText()
                            if (content.isNotBlank()) {
                                Log.d(TAG, "Loaded subfolder .lrc for track: ${song.title}")
                                return LrcParser.parse(content, durationMs = song.durationMs)
                            }
                        }
                    }
                }

                // 4. Sidecar .txt lyrics file
                val matchingTxt = parent.listFiles { file: File ->
                    file.isFile && file.nameWithoutExtension.equals(songBaseName, ignoreCase = true)
                            && file.extension.equals("txt", ignoreCase = true)
                }?.firstOrNull()

                if (matchingTxt != null && matchingTxt.canRead()) {
                    val content = matchingTxt.readText()
                    if (content.isNotBlank()) {
                        Log.d(TAG, "Loaded external .txt lyrics for track: ${song.title}")
                        return LrcParser.parse(content, durationMs = song.durationMs)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "External disk lyrics resolution skipped: ${e.message}")
        }

        return null
    }

    /**
     * Resolves embedded lyrics directly from the audio file tags (.flac Vorbis comment, .mp3 ID3 USLT/SYLT, .m4a ©lyr).
     */
    private fun resolveFromEmbedded(song: SongEntity): LyricsDocument? {
        val path: String = song.filePath
        if (path.isBlank()) return null

        try {
            val rawLyrics = EmbeddedLyricsExtractor.extractLyrics(path)
            if (!rawLyrics.isNullOrBlank()) {
                Log.d(TAG, "Loaded embedded audio file lyrics for track: ${song.title} (${rawLyrics.length} chars)")
                val doc = LrcParser.parse(rawLyrics, durationMs = song.durationMs)
                if (doc.lines.isNotEmpty()) {
                    return doc.copy(
                        title = doc.title ?: song.title,
                        artist = doc.artist ?: song.artist,
                        album = doc.album ?: song.album
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Embedded lyrics extraction error: ${e.message}")
        }

        return null
    }

    fun clearCache() {
        memoryCache.clear()
    }
}

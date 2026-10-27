package com.retro.ipod.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.retro.ipod.data.db.IpodDatabase
import com.retro.ipod.data.db.entity.SongEntity
import com.retro.ipod.data.model.AudioStreamMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * Scans Android's [MediaStore] for audio files and extracts accurate track metadata
 * as well as exact technical stream metrics (Codec, Sample Rate in kHz, PCM Bit Depth).
 */
class AudioMetadataScanner(
    private val context: Context
) {

    companion object {
        private const val TAG = "AudioMetadataScanner"
    }

    data class ScanProgress(
        val scannedCount: Int,
        val totalCount: Int,
        val currentTitle: String? = null
    )

    /**
     * Scans MediaStore and returns the complete list of [SongEntity] models,
     * optionally filtered by a specific directory path.
     */
    suspend fun scanMediaStore(filterFolder: String? = null): List<SongEntity> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<SongEntity>()
        val contentResolver: ContentResolver = context.contentResolver

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val normFilter = filterFolder?.trimEnd('/', '\\')?.replace('\\', '/')

        try {
            contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val duration = cursor.getLong(durationCol)
                    val rawTrack = cursor.getInt(trackCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val mimeType = cursor.getString(mimeCol)

                    // Skip empty or nonexistent paths
                    if (filePath.isBlank() || !File(filePath).exists()) {
                        continue
                    }

                    // Strict folder filter if specified
                    if (normFilter != null && normFilter.isNotBlank()) {
                        val normPath = filePath.replace('\\', '/')
                        if (!normPath.startsWith(normFilter, ignoreCase = true)) {
                            continue
                        }
                    }

                    // Disc & Track Number normalization:
                    // 1. Direct FLAC Vorbis Comment tags (TRACKNUMBER, DISCNUMBER)
                    // 2. MediaStore TRACK column
                    // 3. Android MediaMetadataRetriever fallback
                    val isFlac = filePath.endsWith(".flac", ignoreCase = true) || mimeType?.contains("flac", ignoreCase = true) == true
                    val (discNumber, trackNumber) = if (isFlac) {
                        extractFlacTrackAndDisc(File(filePath)) ?: parseDiscAndTrack(rawTrack).let { (d, t) ->
                            if (t > 0) d to t else (extractTrackAndDiscWithRetriever(filePath) ?: (d to t))
                        }
                    } else {
                        val (d, t) = parseDiscAndTrack(rawTrack)
                        if (t > 0) d to t else (extractTrackAndDiscWithRetriever(filePath) ?: (d to t))
                    }

                    // Fallback titles/artists
                    val fallbackName = File(filePath).nameWithoutExtension
                    val cleanTitle = if (!rawTitle.isNullOrBlank()) rawTitle.trim() else fallbackName
                    val cleanArtist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist.trim() else "Unknown Artist"
                    val cleanAlbum = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum.trim() else "Unknown Album"

                    // Extract exact audio metrics (Codec, Sample Rate, Bit Depth)
                    val metrics = extractAudioMetrics(filePath, mimeType)

                    songList.add(
                        SongEntity(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationMs = duration,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            filePath = filePath,
                            codec = metrics.codec,
                            sampleRateKhz = metrics.sampleRateKhz,
                            bitDepth = metrics.bitDepth
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore audio tracks", e)
        }

        songList
    }

    /**
     * Scans MediaStore with reactive progress emissions for the UI.
     */
    fun scanMediaStoreWithProgress(): Flow<ScanProgress> = flow {
        val songs = scanMediaStore()
        val total = songs.size
        emit(ScanProgress(0, total, null))

        songs.forEachIndexed { index, song ->
            emit(ScanProgress(index + 1, total, song.title))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Scans MediaStore and updates the Room [IpodDatabase] directly.
     * Returns count of tracks inserted.
     */
    suspend fun scanAndSave(database: IpodDatabase): Int = withContext(Dispatchers.IO) {
        val songs = scanMediaStore()
        if (songs.isNotEmpty()) {
            database.musicNavigationDao().insertSongs(songs)
        }
        songs.size
    }

    /**
     * Resolves an Android Storage Access Framework (SAF) tree URI into a standard filesystem path.
     */
    fun resolveStoragePath(treeUri: Uri): String? {
        return try {
            if (android.provider.DocumentsContract.isTreeUri(treeUri)) {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val parts = docId.split(":")
                val storageId = parts[0]
                val subPath = if (parts.size > 1) parts[1] else ""
                if (storageId.equals("primary", ignoreCase = true)) {
                    "/storage/emulated/0/$subPath".trimEnd('/')
                } else {
                    "/storage/$storageId/$subPath".trimEnd('/')
                }
            } else {
                treeUri.path
            }
        } catch (e: Exception) {
            treeUri.path
        }
    }

    /**
     * Scans exclusively within a user-selected folder, combining MediaStore queries
     * with deep filesystem directory traversal to guarantee 100% track detection.
     */
    suspend fun scanFolder(folderPath: String?, treeUri: Uri? = null): List<SongEntity> = withContext(Dispatchers.IO) {
        val resolvedPath = folderPath ?: treeUri?.let { resolveStoragePath(it) }
        if (resolvedPath.isNullOrBlank()) {
            return@withContext scanMediaStore()
        }

        val mediaStoreSongs = scanMediaStore(resolvedPath)
        val knownPaths = mediaStoreSongs.map { it.filePath.replace('\\', '/') }.toHashSet()
        val combinedList = mediaStoreSongs.toMutableList()

        val folderFile = File(resolvedPath)
        if (folderFile.exists() && folderFile.isDirectory) {
            val audioExtensions = setOf("flac", "mp3", "m4a", "wav", "aac", "ogg", "opus", "aiff", "alac")
            folderFile.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                .forEach { file ->
                    val norm = file.absolutePath.replace('\\', '/')
                    if (!knownPaths.contains(norm)) {
                        val singleSong = extractMetadataFromFile(file)
                        if (singleSong != null) {
                            combinedList.add(singleSong)
                            knownPaths.add(norm)
                        }
                    }
                }
        }

        combinedList.sortedWith(
            compareBy<SongEntity> { it.album.lowercase() }
                .thenBy { it.discNumber }
                .thenBy { it.trackNumber }
                .thenBy { it.title.lowercase() }
        )
    }

    /**
     * Extracts a full [SongEntity] directly from a single audio [File] on disk.
     */
    fun extractMetadataFromFile(file: File): SongEntity? {
        if (!file.exists() || !file.canRead() || file.length() <= 0) return null
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            retriever.release()

            val isFlac = file.extension.equals("flac", ignoreCase = true)
            val (disc, track) = if (isFlac) {
                extractFlacTrackAndDisc(file) ?: (1 to resolveTrackNumberFromFilename(file))
            } else {
                extractTrackAndDiscWithRetriever(file.absolutePath) ?: (1 to resolveTrackNumberFromFilename(file))
            }

            val cleanTitle = if (!title.isNullOrBlank()) title else file.nameWithoutExtension
            val cleanArtist = if (!artist.isNullOrBlank() && artist != "<unknown>") artist else "Unknown Artist"
            val cleanAlbum = if (!album.isNullOrBlank() && album != "<unknown>") album else "Unknown Album"
            val metrics = extractAudioMetrics(file.absolutePath, null)

            val rawId = file.absolutePath.hashCode().toLong()
            val safeId = if (rawId < 0) -rawId else rawId

            SongEntity(
                id = safeId,
                title = cleanTitle,
                artist = cleanArtist,
                album = cleanAlbum,
                durationMs = duration,
                trackNumber = track,
                discNumber = disc,
                filePath = file.absolutePath,
                codec = metrics.codec,
                sampleRateKhz = metrics.sampleRateKhz,
                bitDepth = metrics.bitDepth
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata from file ${file.absolutePath}", e)
            null
        }
    }

    private fun resolveTrackNumberFromFilename(file: File): Int {
        val name = file.nameWithoutExtension
        val match = Regex("""^(\d{1,3})[\s\.\-_]""").find(name)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Resolves disc and track numbers.
     * MediaStore frequently stores track numbers formatted as (disc * 1000) + track.
     */
    private fun parseDiscAndTrack(rawTrack: Int): Pair<Int, Int> {
        return when {
            rawTrack >= 1000 -> {
                val disc = rawTrack / 1000
                val track = rawTrack % 1000
                disc to track
            }
            rawTrack > 0 -> {
                1 to rawTrack
            }
            else -> {
                1 to 0
            }
        }
    }

    /**
     * Resolves the true album track number for a song:
     * 1. If song.trackNumber > 0, returns it.
     * 2. If it's a FLAC file, extracts TRACKNUMBER from Vorbis comments.
     * 3. Falls back to MediaMetadataRetriever CD_TRACK_NUMBER.
     * 4. Falls back to filename number prefix (e.g., "01 - Song.flac", "02. Song.flac").
     */
    fun resolveTrackNumber(song: SongEntity): Int {
        if (song.trackNumber > 0) return song.trackNumber

        val file = File(song.filePath)
        if (file.exists() && file.extension.equals("flac", ignoreCase = true)) {
            val flacPair = extractFlacTrackAndDisc(file)
            if (flacPair != null && flacPair.second > 0) {
                return flacPair.second
            }
        }

        // Try retriever fallback
        extractTrackAndDiscWithRetriever(song.filePath)?.let { (_, t) ->
            if (t > 0) return t
        }

        // Try filename prefix e.g. "01 - " or "01." or "01 "
        val name = file.nameWithoutExtension
        val match = Regex("""^(\d{1,3})[\s\.\-_]""").find(name)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num
        }

        return 0
    }

    /**
     * Reads Vorbis comment tags directly from FLAC metadata blocks.
     * Looks for TRACKNUMBER and DISCNUMBER.
     */
    fun extractFlacTrackAndDisc(file: File): Pair<Int, Int>? {
        if (!file.exists() || !file.canRead() || file.length() < 32) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4)
                raf.readFully(header)
                if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                    val id3Header = ByteArray(6)
                    raf.readFully(id3Header)
                    val id3Size = ((id3Header[2].toInt() and 0x7F) shl 21) or
                            ((id3Header[3].toInt() and 0x7F) shl 14) or
                            ((id3Header[4].toInt() and 0x7F) shl 7) or
                            (id3Header[5].toInt() and 0x7F)
                    raf.seek(10L + id3Size)
                    raf.readFully(header)
                }

                if (header[0] != 0x66.toByte() || header[1] != 0x4C.toByte() ||
                    header[2] != 0x61.toByte() || header[3] != 0x43.toByte()
                ) {
                    return null
                }

                var trackNum: Int? = null
                var discNum: Int? = null

                while (raf.filePointer < raf.length() - 4) {
                    val blockHeader = ByteArray(4)
                    raf.readFully(blockHeader)
                    val isLast = (blockHeader[0].toInt() and 0x80) != 0
                    val blockType = blockHeader[0].toInt() and 0x7F
                    val blockLength = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                            ((blockHeader[2].toInt() and 0xFF) shl 8) or
                            (blockHeader[3].toInt() and 0xFF)

                    if (blockType == 4) { // VORBIS_COMMENT
                        val blockData = ByteArray(blockLength)
                        raf.readFully(blockData)

                        if (blockData.size >= 8) {
                            var offset = 0
                            val vendorLen = (blockData[offset].toInt() and 0xFF) or
                                    ((blockData[offset + 1].toInt() and 0xFF) shl 8) or
                                    ((blockData[offset + 2].toInt() and 0xFF) shl 16) or
                                    ((blockData[offset + 3].toInt() and 0xFF) shl 24)
                            offset += 4 + vendorLen

                            if (offset + 4 <= blockData.size) {
                                val commentCount = (blockData[offset].toInt() and 0xFF) or
                                        ((blockData[offset + 1].toInt() and 0xFF) shl 8) or
                                        ((blockData[offset + 2].toInt() and 0xFF) shl 16) or
                                        ((blockData[offset + 3].toInt() and 0xFF) shl 24)
                                offset += 4

                                for (i in 0 until commentCount) {
                                    if (offset + 4 > blockData.size) break
                                    val len = (blockData[offset].toInt() and 0xFF) or
                                            ((blockData[offset + 1].toInt() and 0xFF) shl 8) or
                                            ((blockData[offset + 2].toInt() and 0xFF) shl 16) or
                                            ((blockData[offset + 3].toInt() and 0xFF) shl 24)
                                    offset += 4
                                    if (len > 0 && offset + len <= blockData.size) {
                                        val entry = String(blockData, offset, len, java.nio.charset.StandardCharsets.UTF_8)
                                        offset += len
                                        val eq = entry.indexOf('=')
                                        if (eq > 0) {
                                            val key = entry.substring(0, eq).trim().uppercase()
                                            val value = entry.substring(eq + 1).trim()
                                            if (key == "TRACKNUMBER" || key == "TRACK") {
                                                val tStr = value.split('/')[0].trim()
                                                trackNum = tStr.toIntOrNull()
                                            } else if (key == "DISCNUMBER" || key == "DISC") {
                                                val dStr = value.split('/')[0].trim()
                                                discNum = dStr.toIntOrNull()
                                            }
                                        }
                                    } else {
                                        break
                                    }
                                }
                            }
                        }
                        break
                    } else {
                        raf.skipBytes(blockLength)
                    }

                    if (isLast) break
                }

                if (trackNum != null && trackNum > 0) {
                    (discNum ?: 1) to trackNum
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fallback to Android MediaMetadataRetriever to parse track and disc numbers.
     */
    private fun extractTrackAndDiscWithRetriever(filePath: String): Pair<Int, Int>? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val trackStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val discStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            retriever.release()

            val track = trackStr?.split('/')?.firstOrNull()?.trim()?.toIntOrNull()
            val disc = discStr?.split('/')?.firstOrNull()?.trim()?.toIntOrNull() ?: 1
            if (track != null && track > 0) disc to track else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts exact stream metrics (Codec, sample rate, bit depth) using binary header parsing
     * with high-precision fallback to Android's MediaExtractor.
     */
    fun extractAudioMetrics(filePath: String, mimeType: String?): AudioStreamMetrics {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            return AudioStreamMetrics.DEFAULT_STANDARD
        }

        val extension = file.extension.lowercase()

        // 1. Direct High-Precision Header Parsing for FLAC
        if (extension == "flac" || mimeType?.contains("flac", ignoreCase = true) == true) {
            val flacMetrics = parseFlacHeader(file)
            if (flacMetrics != null) return flacMetrics
        }

        // 2. Direct Header Parsing for WAV / PCM
        if (extension == "wav" || mimeType?.contains("wav", ignoreCase = true) == true) {
            val wavMetrics = parseWavHeader(file)
            if (wavMetrics != null) return wavMetrics
        }

        // 3. MediaExtractor Inspection (MP3, AAC, M4A, ALAC, OGG, OPUS)
        val extractorMetrics = parseWithMediaExtractor(filePath, extension)
        if (extractorMetrics != null) return extractorMetrics

        // 4. Default Fallback
        val codecFallback = when (extension) {
            "flac" -> "FLAC"
            "wav" -> "WAV"
            "alac" -> "ALAC"
            "m4a", "aac" -> "AAC"
            "ogg" -> "OGG"
            "opus" -> "OPUS"
            else -> "MP3"
        }
        val bitDepthFallback = if (codecFallback == "FLAC" || codecFallback == "WAV") 24 else 16
        return AudioStreamMetrics(codecFallback, 44.1, bitDepthFallback)
    }

    /**
     * Parses the FLAC STREAMINFO metadata block (RFC 2529 / Xiph Standard).
     * Extracts exact bit depth (16, 24, 32) and sample rate.
     */
    private fun parseFlacHeader(file: File): AudioStreamMetrics? {
        try {
            RandomAccessFile(file, "r").use { raf ->
                // Check magic bytes "fLaC" (0x66, 0x4C, 0x61, 0x43)
                val magic = ByteArray(4)
                raf.readFully(magic)
                if (magic[0] != 0x66.toByte() || magic[1] != 0x4C.toByte() ||
                    magic[2] != 0x61.toByte() || magic[3] != 0x43.toByte()
                ) {
                    return null
                }

                // First metadata block header (4 bytes)
                val blockHeader = ByteArray(4)
                raf.readFully(blockHeader)
                val blockType = blockHeader[0].toInt() and 0x7F
                if (blockType != 0) { // 0 is STREAMINFO
                    return null
                }

                val streamInfo = ByteArray(34)
                raf.readFully(streamInfo)

                // Sample rate (20 bits) starting at byte index 10:
                // Byte 10: bits 19..12
                // Byte 11: bits 11..4
                // Byte 12: bits 3..0 (upper 4 bits)
                val b10 = streamInfo[10].toInt() and 0xFF
                val b11 = streamInfo[11].toInt() and 0xFF
                val b12 = streamInfo[12].toInt() and 0xFF
                val sampleRateHz = (b10 shl 12) or (b11 shl 4) or (b12 ushr 4)

                // Bit depth (5 bits):
                // Byte 12: bit 0 (lowest bit)
                // Byte 13: upper 4 bits
                val b13 = streamInfo[13].toInt() and 0xFF
                val bitDepth = (((b12 and 0x01) shl 4) or (b13 ushr 4)) + 1

                val sampleRateKhz = Math.round((sampleRateHz / 1000.0) * 10.0) / 10.0
                return AudioStreamMetrics("FLAC", sampleRateKhz, bitDepth)
            }
        } catch (e: Exception) {
            Log.d(TAG, "FLAC header parse skipped: ${e.message}")
        }
        return null
    }

    /**
     * Parses standard RIFF WAVE chunk headers to read sample rate and bit depth.
     */
    private fun parseWavHeader(file: File): AudioStreamMetrics? {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(12)
                raf.readFully(header)
                // "RIFF" .... "WAVE"
                if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") {
                    return null
                }

                // Search for "fmt " subchunk
                while (raf.filePointer < raf.length() - 8) {
                    val chunkId = ByteArray(4)
                    raf.readFully(chunkId)
                    val chunkLength = Integer.reverseBytes(raf.readInt())

                    if (String(chunkId) == "fmt ") {
                        val fmtData = ByteArray(chunkLength)
                        raf.readFully(fmtData)

                        // Sample rate at offset 4 (4 bytes, little-endian)
                        val sampleRateHz = (fmtData[4].toInt() and 0xFF) or
                                ((fmtData[5].toInt() and 0xFF) shl 8) or
                                ((fmtData[6].toInt() and 0xFF) shl 16) or
                                ((fmtData[7].toInt() and 0xFF) shl 24)

                        // Bits per sample at offset 14 (2 bytes, little-endian)
                        val bitsPerSample = (fmtData[14].toInt() and 0xFF) or
                                ((fmtData[15].toInt() and 0xFF) shl 8)

                        val sampleRateKhz = Math.round((sampleRateHz / 1000.0) * 10.0) / 10.0
                        return AudioStreamMetrics("WAV", sampleRateKhz, bitsPerSample)
                    } else {
                        raf.skipBytes(chunkLength)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "WAV header parse skipped: ${e.message}")
        }
        return null
    }

    /**
     * Inspects audio file format using Android's MediaExtractor.
     */
    private fun parseWithMediaExtractor(filePath: String, extension: String): AudioStreamMetrics? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(filePath)
            var metrics: AudioStreamMetrics? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("audio/")) {
                    val sampleRateHz = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    } else {
                        44100
                    }

                    val sampleRateKhz = Math.round((sampleRateHz / 1000.0) * 10.0) / 10.0

                    val bitDepth = when {
                        format.containsKey("bit-width") -> format.getInteger("bit-width")
                        format.containsKey("bits-per-sample") -> format.getInteger("bits-per-sample")
                        format.containsKey(MediaFormat.KEY_PCM_ENCODING) -> {
                            when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                                AudioFormat.ENCODING_PCM_8BIT -> 8
                                AudioFormat.ENCODING_PCM_16BIT -> 16
                                AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                                AudioFormat.ENCODING_PCM_32BIT -> 32
                                AudioFormat.ENCODING_PCM_FLOAT -> 32
                                else -> 16
                            }
                        }
                        else -> 16
                    }

                    val codec = when {
                        mime.contains("flac", ignoreCase = true) -> "FLAC"
                        mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> "MP3"
                        mime.contains("raw", ignoreCase = true) || mime.contains("wav", ignoreCase = true) -> "WAV"
                        mime.contains("alac", ignoreCase = true) -> "ALAC"
                        mime.contains("mp4", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
                        mime.contains("vorbis", ignoreCase = true) || mime.contains("ogg", ignoreCase = true) -> "OGG"
                        mime.contains("opus", ignoreCase = true) -> "OPUS"
                        else -> extension.uppercase()
                    }

                    metrics = AudioStreamMetrics(codec, sampleRateKhz, bitDepth)
                    break
                }
            }
            metrics
        } catch (e: Exception) {
            null
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) { }
        }
    }
}

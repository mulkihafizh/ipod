package com.retro.ipod.lyrics.parser

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * High-performance, zero-dependency extractor for embedded audio file lyrics.
 * Supports:
 * - FLAC: Vorbis Comment blocks (RFC 2529 / Xiph Standard) containing LYRICS, UNSYNCEDLYRICS, etc.
 * - MP3: ID3v2.3 and ID3v2.4 USLT (Unsynchronized lyrics) and SYLT frames.
 * - M4A / ALAC / MP4: iTunes metadata '©lyr' atom inside moov/udta/meta/ilst.
 */
object EmbeddedLyricsExtractor {

    private const val TAG = "EmbeddedLyricsExtractor"

    private val TARGET_VORBIS_KEYS = setOf(
        "LYRICS",
        "UNSYNCEDLYRICS",
        "SYNCEDLYRICS",
        "UNSYNCED LYRICS",
        "LYRICS-ENG",
        "LYRICS-KOR",
        "LYRICS-JA"
    )

    /**
     * Extracts embedded lyrics from an audio file on disk, if present.
     * Returns the raw lyrics string (which can be standard LRC, enhanced LRC, or plain text).
     */
    fun extractLyrics(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() < 32) {
            return null
        }

        val ext = file.extension.lowercase()
        return try {
            when (ext) {
                "flac" -> extractFromFlac(file)
                "mp3" -> extractFromMp3(file)
                "m4a", "mp4", "alac", "aac" -> extractFromMp4(file)
                "ogg", "opus" -> extractFromOgg(file)
                else -> {
                    // Try FLAC then MP3 ID3 as fallback
                    extractFromFlac(file) ?: extractFromMp3(file)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Lyrics extraction skipped for $filePath: ${e.message}")
            null
        }
    }

    /**
     * Extracts lyrics from FLAC Vorbis Comment metadata blocks.
     */
    fun extractFromFlac(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(4)
            raf.readFully(header)

            // Handle ID3v2 prepend header if present
            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                val id3Len = skipId3v2Header(raf)
                raf.seek(id3Len)
                raf.readFully(header)
            }

            // Verify "fLaC" magic bytes
            if (header[0] != 0x66.toByte() || header[1] != 0x4C.toByte() ||
                header[2] != 0x61.toByte() || header[3] != 0x43.toByte()
            ) {
                return null
            }

            // Traverse FLAC metadata blocks
            while (raf.filePointer < raf.length() - 4) {
                val blockHeader = ByteArray(4)
                raf.readFully(blockHeader)

                val isLast = (blockHeader[0].toInt() and 0x80) != 0
                val blockType = blockHeader[0].toInt() and 0x7F
                val blockLength = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                        ((blockHeader[2].toInt() and 0xFF) shl 8) or
                        (blockHeader[3].toInt() and 0xFF)

                if (blockType == 4) { // VORBIS_COMMENT block
                    val blockData = ByteArray(blockLength)
                    raf.readFully(blockData)
                    val lyrics = parseVorbisCommentBlock(blockData)
                    if (!lyrics.isNullOrBlank()) {
                        return lyrics
                    }
                } else {
                    raf.skipBytes(blockLength)
                }

                if (isLast) break
            }
        }
        return null
    }

    private fun parseVorbisCommentBlock(data: ByteArray): String? {
        if (data.size < 8) return null
        var pos = 0

        // 1. Vendor string length (4 bytes, little-endian)
        val vendorLen = readInt32LE(data, pos)
        pos += 4 + vendorLen
        if (pos + 4 > data.size) return null

        // 2. User comment count (4 bytes, little-endian)
        val userCommentCount = readInt32LE(data, pos)
        pos += 4

        // 3. User comments
        for (i in 0 until userCommentCount) {
            if (pos + 4 > data.size) break
            val commentLen = readInt32LE(data, pos)
            pos += 4
            if (pos + commentLen > data.size) break

            val commentStr = String(data, pos, commentLen, StandardCharsets.UTF_8)
            pos += commentLen

            val separatorIdx = commentStr.indexOf('=')
            if (separatorIdx > 0) {
                val key = commentStr.substring(0, separatorIdx).trim().uppercase()
                val value = commentStr.substring(separatorIdx + 1).trim()
                if (key in TARGET_VORBIS_KEYS && value.isNotBlank()) {
                    return value
                }
            }
        }
        return null
    }

    /**
     * Extracts lyrics from MP3 ID3v2 USLT (Unsynchronized lyrics) frames.
     */
    fun extractFromMp3(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val id3Header = ByteArray(10)
            raf.readFully(id3Header)

            if (id3Header[0] != 'I'.code.toByte() || id3Header[1] != 'D'.code.toByte() || id3Header[2] != '3'.code.toByte()) {
                return null
            }

            val versionMajor = id3Header[3].toInt() and 0xFF
            val tagSize = ((id3Header[6].toInt() and 0x7F) shl 21) or
                    ((id3Header[7].toInt() and 0x7F) shl 14) or
                    ((id3Header[8].toInt() and 0x7F) shl 7) or
                    (id3Header[9].toInt() and 0x7F)

            val tagEnd = 10L + tagSize
            val maxRead = minOf(tagEnd, raf.length())

            while (raf.filePointer < maxRead - 10) {
                val frameHeader = ByteArray(10)
                raf.readFully(frameHeader)

                // Frame ID (4 ASCII chars)
                val frameId = String(frameHeader, 0, 4, StandardCharsets.US_ASCII)
                if (frameId.isBlank() || frameId[0] == '\u0000') break

                // Frame size
                val frameSize = if (versionMajor >= 4) {
                    ((frameHeader[4].toInt() and 0x7F) shl 21) or
                            ((frameHeader[5].toInt() and 0x7F) shl 14) or
                            ((frameHeader[6].toInt() and 0x7F) shl 7) or
                            (frameHeader[7].toInt() and 0x7F)
                } else {
                    ((frameHeader[4].toInt() and 0xFF) shl 24) or
                            ((frameHeader[5].toInt() and 0xFF) shl 16) or
                            ((frameHeader[6].toInt() and 0xFF) shl 8) or
                            (frameHeader[7].toInt() and 0xFF)
                }

                if (frameSize <= 0 || raf.filePointer + frameSize > maxRead) {
                    break
                }

                if (frameId == "USLT") {
                    val frameData = ByteArray(frameSize)
                    raf.readFully(frameData)
                    val lyrics = parseUsltFrame(frameData)
                    if (!lyrics.isNullOrBlank()) {
                        return lyrics
                    }
                } else if (frameId == "SYLT") {
                    val frameData = ByteArray(frameSize)
                    raf.readFully(frameData)
                    val lyrics = parseSyltFrame(frameData)
                    if (!lyrics.isNullOrBlank()) {
                        return lyrics
                    }
                } else {
                    raf.skipBytes(frameSize)
                }
            }
        }
        return null
    }

    private fun parseUsltFrame(data: ByteArray): String? {
        if (data.size < 5) return null
        val encodingByte = data[0].toInt() and 0xFF
        val charset = getCharsetFromId3Encoding(encodingByte)

        // Skip 1 byte encoding + 3 bytes language = 4 bytes
        var pos = 4
        // Skip content descriptor (terminated by null character)
        pos = skipNullTerminated(data, pos, encodingByte)
        if (pos >= data.size) return null

        return String(data, pos, data.size - pos, charset).trim()
    }

    private fun parseSyltFrame(data: ByteArray): String? {
        if (data.size < 6) return null
        val encodingByte = data[0].toInt() and 0xFF
        val charset = getCharsetFromId3Encoding(encodingByte)

        // 1 byte encoding + 3 bytes language + 1 byte timestamp format + 1 byte content type = 6 bytes
        var pos = 6
        // Skip descriptor
        pos = skipNullTerminated(data, pos, encodingByte)
        if (pos >= data.size) return null

        // Build standard LRC string from SYLT entries
        val sb = StringBuilder()
        while (pos < data.size) {
            val textStart = pos
            pos = skipNullTerminated(data, pos, encodingByte)
            val lineText = String(data, textStart, (pos - textStart).coerceAtLeast(0), charset).trim()
            if (pos + 4 <= data.size) {
                val timeMs = ((data[pos].toInt() and 0xFF) shl 24) or
                        ((data[pos + 1].toInt() and 0xFF) shl 16) or
                        ((data[pos + 2].toInt() and 0xFF) shl 8) or
                        (data[pos + 3].toInt() and 0xFF)
                pos += 4
                if (lineText.isNotBlank()) {
                    val min = (timeMs / 1000) / 60
                    val sec = (timeMs / 1000) % 60
                    val hundredths = (timeMs % 1000) / 10
                    sb.append(String.format("[%02d:%02d.%02d]%s\n", min, sec, hundredths, lineText))
                }
            } else break
        }

        return if (sb.isNotEmpty()) sb.toString() else null
    }

    /**
     * Extracts lyrics from MP4 / M4A / ALAC iTunes metadata atom '©lyr'.
     */
    fun extractFromMp4(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            return findMp4Atom(raf, 0, raf.length(), "©lyr")
        }
    }

    private fun findMp4Atom(raf: RandomAccessFile, startOffset: Long, endOffset: Long, targetType: String): String? {
        raf.seek(startOffset)
        while (raf.filePointer < endOffset - 8) {
            val atomHeader = ByteArray(8)
            raf.readFully(atomHeader)
            var atomSize = ((atomHeader[0].toLong() and 0xFF) shl 24) or
                    ((atomHeader[1].toLong() and 0xFF) shl 16) or
                    ((atomHeader[2].toLong() and 0xFF) shl 8) or
                    (atomHeader[3].toLong() and 0xFF)
            val atomType = String(atomHeader, 4, 4, StandardCharsets.ISO_8859_1)

            if (atomSize == 1L) { // 64-bit size
                val extendedSize = ByteArray(8)
                raf.readFully(extendedSize)
                atomSize = 0L
                for (b in extendedSize) {
                    atomSize = (atomSize shl 8) or (b.toLong() and 0xFF)
                }
            }

            if (atomSize < 8) break
            val currentAtomStart = raf.filePointer - 8
            val currentAtomEnd = currentAtomStart + atomSize

            if (atomType == "moov" || atomType == "udta" || atomType == "meta" || atomType == "ilst") {
                val skipInner = if (atomType == "meta") 4 else 0 // meta atom has 4 bytes version/flags
                val result = findMp4Atom(raf, raf.filePointer + skipInner, currentAtomEnd, targetType)
                if (result != null) return result
            } else if (atomType == targetType || atomType == "\u00A9lyr") {
                // Inside ©lyr atom, look for 'data' child atom
                while (raf.filePointer < currentAtomEnd - 8) {
                    val childHeader = ByteArray(8)
                    raf.readFully(childHeader)
                    val childSize = ((childHeader[0].toInt() and 0xFF) shl 24) or
                            ((childHeader[1].toInt() and 0xFF) shl 16) or
                            ((childHeader[2].toInt() and 0xFF) shl 8) or
                            (childHeader[3].toInt() and 0xFF)
                    val childType = String(childHeader, 4, 4, StandardCharsets.ISO_8859_1)

                    if (childType == "data") {
                        // Skip 8 bytes (version, flags, reserved)
                        raf.skipBytes(8)
                        val dataLen = (childSize - 16).coerceAtLeast(0)
                        val textBytes = ByteArray(dataLen)
                        raf.readFully(textBytes)
                        return String(textBytes, StandardCharsets.UTF_8).trim()
                    } else if (childSize > 8) {
                        raf.skipBytes(childSize - 8)
                    } else break
                }
            }

            raf.seek(currentAtomEnd)
        }
        return null
    }

    /**
     * Extracts lyrics from OGG Vorbis / Opus comment packets.
     */
    fun extractFromOgg(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            val buf = ByteArray(minOf(file.length().toInt(), 64 * 1024))
            raf.readFully(buf)
            val text = String(buf, StandardCharsets.ISO_8859_1)
            for (key in TARGET_VORBIS_KEYS) {
                val idx = text.indexOf("$key=", ignoreCase = true)
                if (idx >= 0) {
                    val valueStart = idx + key.length + 1
                    var valueEnd = text.indexOf('\u0000', valueStart)
                    if (valueEnd < 0) valueEnd = (valueStart + 4000).coerceAtMost(text.length)
                    val rawValue = text.substring(valueStart, valueEnd)
                    val utf8Bytes = rawValue.toByteArray(StandardCharsets.ISO_8859_1)
                    val lyrics = String(utf8Bytes, StandardCharsets.UTF_8).trim()
                    if (lyrics.isNotBlank()) return lyrics
                }
            }
        }
        return null
    }

    private fun skipId3v2Header(raf: RandomAccessFile): Long {
        val bytes = ByteArray(6)
        raf.readFully(bytes)
        val tagSize = ((bytes[2].toInt() and 0x7F) shl 21) or
                ((bytes[3].toInt() and 0x7F) shl 14) or
                ((bytes[4].toInt() and 0x7F) shl 7) or
                (bytes[5].toInt() and 0x7F)
        return 10L + tagSize
    }

    private fun getCharsetFromId3Encoding(encodingByte: Int): Charset {
        return when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
    }

    private fun skipNullTerminated(data: ByteArray, start: Int, encodingByte: Int): Int {
        var pos = start
        if (encodingByte == 1 || encodingByte == 2) { // 2-byte null terminator
            while (pos + 1 < data.size) {
                if (data[pos] == 0.toByte() && data[pos + 1] == 0.toByte()) {
                    return pos + 2
                }
                pos += 2
            }
        } else { // 1-byte null terminator
            while (pos < data.size) {
                if (data[pos] == 0.toByte()) {
                    return pos + 1
                }
                pos++
            }
        }
        return pos
    }

    private fun readInt32LE(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
}

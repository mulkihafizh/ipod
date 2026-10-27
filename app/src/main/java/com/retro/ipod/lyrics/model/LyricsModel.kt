package com.retro.ipod.lyrics.model

/**
 * Single syllable or word with high-precision timestamp boundaries in milliseconds.
 */
data class LyricWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    /**
     * Normalized progress in [0.0, 1.0] representing how much of this word has elapsed.
     */
    fun progress(currentPositionMs: Long): Float {
        if (currentPositionMs < startTimeMs) return 0f
        if (currentPositionMs >= endTimeMs) return 1f
        val duration = endTimeMs - startTimeMs
        return if (duration > 0) {
            ((currentPositionMs - startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
        } else 1f
    }

    fun isCurrentlySung(currentPositionMs: Long): Boolean {
        return currentPositionMs in startTimeMs until endTimeMs
    }

    fun isPast(currentPositionMs: Long): Boolean {
        return currentPositionMs >= endTimeMs && endTimeMs > startTimeMs
    }
}

/**
 * Single lyrical line containing word-level syllables (if enhanced LRC) or plain line-level timestamps.
 */
data class LyricLine(
    val lineIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sub: List<String> = emptyList(),
    val isBackingVocal: Boolean = false,
    val isGap: Boolean = false,
    val alignRight: Boolean = false
) {
    val hasSyllables: Boolean
        get() = words.isNotEmpty()

    fun isActive(currentPositionMs: Long): Boolean {
        return currentPositionMs in startTimeMs until endTimeMs
    }

    fun isPast(currentPositionMs: Long): Boolean {
        return currentPositionMs >= endTimeMs
    }
}

/**
 * Complete parsed lyrics document with line and syllable support.
 */
data class LyricsDocument(
    val lines: List<LyricLine> = emptyList(),
    val hasSyllables: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null
) {
    /**
     * Resolves the active line index for a given playback position in milliseconds.
     * Ensures smooth focus progression without ever falling back to index 0 during
     * instrumental pauses or gaps between lines.
     */
    fun findActiveLineIndex(currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (currentPositionMs < lines.first().startTimeMs) return 0
        if (currentPositionMs >= lines.last().startTimeMs) return lines.lastIndex

        // Binary search for the highest line index whose startTimeMs <= currentPositionMs
        var low = 0
        var high = lines.size - 1
        var result = 0
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (lines[mid].startTimeMs <= currentPositionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}

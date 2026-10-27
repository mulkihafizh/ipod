package com.retro.ipod.lyrics.parser

import com.retro.ipod.lyrics.model.LyricLine
import com.retro.ipod.lyrics.model.LyricWord
import com.retro.ipod.lyrics.model.LyricsDocument
import java.util.regex.Pattern

/**
 * High-performance parser for Enhanced LRC (syllable-level <mm:ss.xx> word tags),
 * standard plain LRC ([mm:ss.xx] line tags), and unsynced plain lyrics text.
 */
object LrcParser {

    private val LINE_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})[\\.:](\\d{2,3})\\]")
    private val WORD_TAG_PATTERN = Pattern.compile("<(\\d{1,2}):(\\d{2})[\\.:](\\d{2,3})>([^<]*)")
    private val META_TAG_PATTERN = Pattern.compile("^\\[(ti|ar|al|by|offset|length):([^\\]]+)\\]$", Pattern.CASE_INSENSITIVE)

    /**
     * Parses an LRC string content or embedded lyrics into a structured [LyricsDocument].
     */
    fun parse(content: String?, durationMs: Long = 0L): LyricsDocument {
        if (content.isNullOrBlank()) {
            return LyricsDocument()
        }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var offsetMs: Long = 0L

        val rawCollectedLines = mutableListOf<LyricLine>()
        var hasSyllablesOverall = false

        val rawLines = content.lines()
        for (raw in rawLines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            // 1. Check metadata headers: [ti:...], [ar:...], [offset:...]
            val metaMatcher = META_TAG_PATTERN.matcher(line)
            if (metaMatcher.matches()) {
                val key = metaMatcher.group(1)?.lowercase()
                val value = metaMatcher.group(2)?.trim()
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value?.toLongOrNull() ?: 0L
                }
                continue
            }

            // 2. Extract all leading timestamps on the line: e.g. [00:12.34][01:23.45]Hello
            val matcher = LINE_TAG_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastTagEnd = 0

            while (matcher.find()) {
                if (matcher.start() == lastTagEnd) {
                    val min = matcher.group(1)?.toLongOrNull() ?: 0L
                    val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                    val msStr = matcher.group(3) ?: "00"
                    val ms = if (msStr.length == 2) msStr.toLong() * 10L else msStr.toLong()
                    val timeMs = (min * 60L + sec) * 1000L + ms + offsetMs
                    timestamps.add(timeMs.coerceAtLeast(0L))
                    lastTagEnd = matcher.end()
                } else {
                    break
                }
            }

            if (timestamps.isEmpty()) {
                continue
            }

            val lineBody = line.substring(lastTagEnd).trim()

            // If a timestamp-only line is found (e.g. [01:20.00] with blank text),
            // it denotes the end boundary of the preceding lyric line.
            if (lineBody.isEmpty()) {
                if (rawCollectedLines.isNotEmpty()) {
                    val lastIdx = rawCollectedLines.lastIndex
                    val prev = rawCollectedLines[lastIdx]
                    val closedEndTime = timestamps.first()
                    if (closedEndTime > prev.startTimeMs) {
                        rawCollectedLines[lastIdx] = prev.copy(endTimeMs = closedEndTime)
                    }
                }
                continue
            }

            // Parse word timestamps if present (<mm:ss.xx>word)
            val words = mutableListOf<LyricWord>()
            val wordMatcher = WORD_TAG_PATTERN.matcher(lineBody)
            var lastWordExplicitlyClosed = false

            while (wordMatcher.find()) {
                val wMin = wordMatcher.group(1)?.toLongOrNull() ?: 0L
                val wSec = wordMatcher.group(2)?.toLongOrNull() ?: 0L
                val wMsStr = wordMatcher.group(3) ?: "00"
                val wMs = if (wMsStr.length == 2) wMsStr.toLong() * 10L else wMsStr.toLong()
                val parsedWordStartMs = (wMin * 60L + wSec) * 1000L + wMs + offsetMs
                val wordText = wordMatcher.group(4) ?: ""

                // Ensure word timestamps are strictly non-decreasing and bounded by line start
                val wordStartMs = if (words.isNotEmpty()) {
                    maxOf(parsedWordStartMs, words.last().startTimeMs)
                } else {
                    maxOf(parsedWordStartMs, timestamps.first())
                }

                // Chain previous word's end time to this word's start time
                if (words.isNotEmpty()) {
                    val prev = words.last()
                    words[words.lastIndex] = prev.copy(endTimeMs = maxOf(prev.startTimeMs + 40L, wordStartMs))
                }

                if (wordText.isNotBlank()) {
                    words.add(LyricWord(text = wordText, startTimeMs = wordStartMs, endTimeMs = wordStartMs + 1000L))
                    lastWordExplicitlyClosed = false
                } else if (words.isNotEmpty()) {
                    // Empty text in a tag like <00:14.66> indicates explicit line/word close
                    lastWordExplicitlyClosed = true
                }
            }

            if (words.isNotEmpty()) {
                hasSyllablesOverall = true
            }

            // Strip timestamp tags to produce clean display text
            val cleanText = lineBody.replace(Regex("<[^>]+>"), "").trim()
            if (cleanText.isEmpty()) continue

            val isBackingVocal = isStandaloneParentheses(cleanText)

            // Add an entry for each timestamp tag on this line
            for (ts in timestamps) {
                rawCollectedLines.add(
                    LyricLine(
                        lineIndex = 0, // Assigned after sorting
                        startTimeMs = ts,
                        endTimeMs = ts + 4000L, // Initial estimate, sealed below
                        text = cleanText,
                        words = if (ts == timestamps.first()) words else emptyList(),
                        isBackingVocal = isBackingVocal,
                        alignRight = isBackingVocal
                    )
                )
            }
        }

        // 3. Fallback for plain unsynced lyrics (if no timestamped lines could be parsed)
        if (rawCollectedLines.isEmpty()) {
            return parsePlainLyrics(content, durationMs, title, artist, album)
        }

        // 4. Sort chronologically by startTimeMs
        rawCollectedLines.sortBy { it.startTimeMs }

        // Pair entries with duplicate/near-identical timestamps (within 5ms, e.g. romanized lines or translations)
        val pairedEntries = mutableListOf<LyricLine>()
        var n = 0
        while (n < rawCollectedLines.size) {
            val curr = rawCollectedLines[n]
            val subList = mutableListOf<String>()

            while (n + 1 < rawCollectedLines.size && Math.abs(rawCollectedLines[n + 1].startTimeMs - curr.startTimeMs) <= 5L) {
                val nextBare = rawCollectedLines[n + 1].text.trim()
                if (nextBare.isNotEmpty() && nextBare != curr.text.trim()) {
                    subList.add(nextBare)
                }
                n++
            }

            val isParenthetical = isStandaloneParentheses(curr.text)
            pairedEntries.add(
                curr.copy(
                    sub = subList,
                    isBackingVocal = isParenthetical,
                    alignRight = isParenthetical
                )
            )
            n++
        }

        // 5. Seal line durations and assign consistent 0-based indices
        val finalLines = mutableListOf<LyricLine>()
        for (i in pairedEntries.indices) {
            val curr = pairedEntries[i]
            val nextStart = if (i + 1 < pairedEntries.size) pairedEntries[i + 1].startTimeMs else null

            val sealedEndTime = when {
                curr.endTimeMs > curr.startTimeMs && (nextStart == null || curr.endTimeMs < nextStart) -> {
                    curr.endTimeMs
                }
                nextStart != null -> {
                    // Line duration bounded between 1s and 8s, or until the next line starts
                    val maxAllowed = curr.startTimeMs + 8000L
                    if (curr.words.isNotEmpty()) {
                        val lastWordEnd = curr.words.last().endTimeMs
                        maxOf(lastWordEnd, minOf(nextStart, maxAllowed))
                    } else {
                        minOf(nextStart, maxAllowed)
                    }
                }
                else -> {
                    if (curr.words.isNotEmpty()) {
                        maxOf(curr.words.last().endTimeMs, curr.startTimeMs + 4000L)
                    } else {
                        curr.startTimeMs + 4000L
                    }
                }
            }

            // If the line has syllables, ensure the final word properly extends to sealedEndTime if not explicitly closed
            val sealedWords = if (curr.words.isNotEmpty()) {
                val modWords = curr.words.toMutableList()
                val lastWord = modWords.last()
                val wordEnd = if (lastWord.endTimeMs <= lastWord.startTimeMs + 1000L && sealedEndTime > lastWord.startTimeMs) {
                    minOf(sealedEndTime, lastWord.startTimeMs + 3000L)
                } else {
                    minOf(maxOf(lastWord.endTimeMs, lastWord.startTimeMs + 200L), sealedEndTime)
                }
                modWords[modWords.lastIndex] = lastWord.copy(
                    startTimeMs = maxOf(lastWord.startTimeMs, curr.startTimeMs),
                    endTimeMs = maxOf(wordEnd, lastWord.startTimeMs + 100L)
                )
                modWords
            } else {
                curr.words
            }

            finalLines.add(
                curr.copy(
                    lineIndex = i,
                    endTimeMs = sealedEndTime,
                    words = sealedWords
                )
            )
        }

        return LyricsDocument(
            lines = finalLines,
            hasSyllables = hasSyllablesOverall,
            title = title,
            artist = artist,
            album = album
        )
    }

    /**
     * Parses unsynced plain text lyrics by distributing lines evenly across the song's duration.
     */
    private fun parsePlainLyrics(
        content: String,
        durationMs: Long,
        title: String?,
        artist: String?,
        album: String?
    ): LyricsDocument {
        val nonBlank = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("[") }

        if (nonBlank.isEmpty()) {
            return LyricsDocument(title = title, artist = artist, album = album)
        }

        val totalMs = if (durationMs > 10000L) durationMs else 180000L
        val intervalMs = (totalMs / nonBlank.size).coerceIn(2500L, 8000L)
        var cursorMs = 2000L

        val lines = nonBlank.mapIndexed { idx, text ->
            val start = cursorMs
            val end = (start + intervalMs - 500L).coerceAtLeast(start + 1500L)
            cursorMs += intervalMs
            LyricLine(
                lineIndex = idx,
                startTimeMs = start,
                endTimeMs = end,
                text = text,
                isBackingVocal = text.startsWith("(") && text.endsWith(")")
            )
        }

        return LyricsDocument(
            lines = lines,
            hasSyllables = false,
            title = title,
            artist = artist,
            album = album
        )
    }

    /**
     * Generates high-fidelity demonstration enhanced syllable lyrics for a given track,
     * allowing immediate testing of the word sweep engine even if no external .lrc file exists.
     */
    fun generateDemoLyrics(title: String, artist: String, durationMs: Long): LyricsDocument {
        val totalSec = if (durationMs > 0) durationMs / 1000L else 180L
        val lines = mutableListOf<LyricLine>()

        val templatePhrases = listOf(
            listOf("Step", "into", "the", "timeless", "sound"),
            listOf("Where", "retro", "memories", "are", "found"),
            listOf("Direct", "stream", "flow", "in", "high", "fidelity"),
            listOf("Crystal", "clear", "sound", "for", "you", "and", "me"),
            listOf("Feel", "the", "rhythm", "take", "control"),
            listOf("Pure", "music", "deep", "inside", "your", "soul"),
            listOf("Spin", "the", "wheel", "and", "let", "it", "play"),
            listOf("Wash", "the", "silent", "night", "away"),
            listOf("(Whispering", "melodies", "in", "the", "air)"),
            listOf("Nothing", "in", "this", "world", "can", "compare"),
            listOf("Golden", "hours", "turning", "slow"),
            listOf("Underneath", "the", "aurora", "glow")
        )

        var currentTimeMs = 4000L
        val stepInterval = 4500L
        var lineIdx = 0

        while (currentTimeMs < (totalSec * 1000L - 6000L) && lineIdx < templatePhrases.size * 3) {
            val phrase = templatePhrases[lineIdx % templatePhrases.size]
            val lineStart = currentTimeMs
            val lineDuration = (stepInterval - 600L).coerceAtLeast(2000L)
            val lineEnd = lineStart + lineDuration

            val wordDuration = lineDuration / phrase.size
            val words = mutableListOf<LyricWord>()
            var wordCursor = lineStart

            for (w in phrase) {
                val wEnd = wordCursor + wordDuration
                words.add(LyricWord(text = w, startTimeMs = wordCursor, endTimeMs = wEnd))
                wordCursor = wEnd
            }

            val cleanText = phrase.joinToString(" ")
            val isBacking = cleanText.startsWith("(")

            lines.add(
                LyricLine(
                    lineIndex = lineIdx,
                    startTimeMs = lineStart,
                    endTimeMs = lineEnd,
                    text = cleanText,
                    words = words,
                    isBackingVocal = isBacking
                )
            )

            currentTimeMs += stepInterval
            lineIdx++
        }

        return LyricsDocument(
            lines = lines,
            hasSyllables = true,
            title = title,
            artist = artist
        )
    }

    /**
     * Checks if line is parenthetical backing vocals matching the JS reference:
     * ^[\(\uff08].*[\)\uff09][\.\?!,\u2026]*$
     */
    fun isStandaloneParentheses(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        var s = text.trim()
        s = s.replace(Regex("^[\"'\u201c\u2018\u300c]+|[\"'\u201d\u2019\u300d]+$"), "").trim()
        return Regex("^[\\(\\uff08].*[\\)\\uff09][\\.\\?!,\\u2026]*$").matches(s)
    }
}

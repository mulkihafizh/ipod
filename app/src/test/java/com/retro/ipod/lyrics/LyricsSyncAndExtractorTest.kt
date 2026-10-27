package com.retro.ipod.lyrics

import com.retro.ipod.lyrics.model.LyricLine
import com.retro.ipod.lyrics.model.LyricsDocument
import com.retro.ipod.lyrics.parser.EmbeddedLyricsExtractor
import com.retro.ipod.lyrics.parser.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class LyricsSyncAndExtractorTest {

    @Test
    fun testActiveLineIndexDuringInstrumentalGaps() {
        // Line 0: 10s -> 15s
        // Line 1: 20s -> 25s (5s gap between 15s and 20s!)
        // Line 2: 40s -> 45s (15s instrumental solo between 25s and 40s!)
        val doc = LyricsDocument(
            lines = listOf(
                LyricLine(lineIndex = 0, startTimeMs = 10000L, endTimeMs = 15000L, text = "Line 0"),
                LyricLine(lineIndex = 1, startTimeMs = 20000L, endTimeMs = 25000L, text = "Line 1"),
                LyricLine(lineIndex = 2, startTimeMs = 40000L, endTimeMs = 45000L, text = "Line 2")
            )
        )

        // 1. Before track starts / during intro (0s - 9.9s): index 0
        assertEquals(0, doc.findActiveLineIndex(0L))
        assertEquals(0, doc.findActiveLineIndex(5000L))
        assertEquals(0, doc.findActiveLineIndex(9999L))

        // 2. During Line 0 singing (10s - 14.9s): index 0
        assertEquals(0, doc.findActiveLineIndex(10000L))
        assertEquals(0, doc.findActiveLineIndex(12500L))
        assertEquals(0, doc.findActiveLineIndex(14999L))

        // 3. CRITICAL BUG FIX VERIFICATION:
        // During the 5-second gap between Line 0 and Line 1 (15s - 19.9s):
        // Previously this erroneously returned 0 (snapping back to line 0)!
        // It must now remain steady at Line 0 until Line 1 starts!
        assertEquals(0, doc.findActiveLineIndex(15000L))
        assertEquals(0, doc.findActiveLineIndex(17500L))
        assertEquals(0, doc.findActiveLineIndex(19999L))

        // 4. Line 1 starts (20s): index 1
        assertEquals(1, doc.findActiveLineIndex(20000L))
        assertEquals(1, doc.findActiveLineIndex(22000L))

        // 5. CRITICAL BUG FIX VERIFICATION:
        // During the 15-second guitar solo between Line 1 and Line 2 (25s - 39.9s):
        // Previously this erroneously returned 0 (snapping all the way back to the first line)!
        // It must now remain rock-steady at Line 1!
        assertEquals(1, doc.findActiveLineIndex(25000L))
        assertEquals(1, doc.findActiveLineIndex(30000L))
        assertEquals(1, doc.findActiveLineIndex(35000L))
        assertEquals(1, doc.findActiveLineIndex(39999L))

        // 6. Line 2 starts (40s): index 2
        assertEquals(2, doc.findActiveLineIndex(40000L))
        assertEquals(2, doc.findActiveLineIndex(44000L))

        // 7. After Line 2 ends (outro): index 2
        assertEquals(2, doc.findActiveLineIndex(46000L))
        assertEquals(2, doc.findActiveLineIndex(100000L))
    }

    @Test
    fun testLrcParserMultipleTimestampsAndGaps() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [00:05.00][00:25.00]Repeated Chorus Line
            [00:15.00]Verse Line 1
            [00:20.00]
            [00:35.00]After Bridge
        """.trimIndent()

        val doc = LrcParser.parse(lrc, durationMs = 60000L)

        assertEquals("Test Title", doc.title)
        assertEquals("Test Artist", doc.artist)

        // Lines should be sorted chronologically:
        // 0: 5000ms -> Repeated Chorus Line
        // 1: 15000ms -> Verse Line 1
        // (20000ms had empty text, so it closed line 1's endTimeMs to 20000ms)
        // 2: 25000ms -> Repeated Chorus Line
        // 3: 35000ms -> After Bridge
        assertEquals(4, doc.lines.size)

        assertEquals(0, doc.lines[0].lineIndex)
        assertEquals(5000L, doc.lines[0].startTimeMs)
        assertEquals("Repeated Chorus Line", doc.lines[0].text)

        assertEquals(1, doc.lines[1].lineIndex)
        assertEquals(15000L, doc.lines[1].startTimeMs)
        assertEquals(20000L, doc.lines[1].endTimeMs) // Sealed by the [00:20.00] empty line!
        assertEquals("Verse Line 1", doc.lines[1].text)

        assertEquals(2, doc.lines[2].lineIndex)
        assertEquals(25000L, doc.lines[2].startTimeMs)
        assertEquals("Repeated Chorus Line", doc.lines[2].text)

        assertEquals(3, doc.lines[3].lineIndex)
        assertEquals(35000L, doc.lines[3].startTimeMs)
        assertEquals("After Bridge", doc.lines[3].text)
    }

    @Test
    fun testPlainLyricsFallback() {
        val plain = """
            First verse line
            Second verse line
            Third verse line
        """.trimIndent()

        val doc = LrcParser.parse(plain, durationMs = 30000L)

        assertEquals(3, doc.lines.size)
        assertFalse(doc.hasSyllables)
        assertEquals("First verse line", doc.lines[0].text)
        assertEquals("Second verse line", doc.lines[1].text)
        assertEquals("Third verse line", doc.lines[2].text)
        assertTrue(doc.lines[1].startTimeMs > doc.lines[0].startTimeMs)
        assertTrue(doc.lines[2].startTimeMs > doc.lines[1].startTimeMs)
    }

    @Test
    fun testFlacVorbisCommentExtraction() {
        // Construct a synthetic minimal FLAC file with a VORBIS_COMMENT block containing LYRICS tag
        val tempFile = File.createTempFile("test_audio", ".flac")
        tempFile.deleteOnExit()

        FileOutputStream(tempFile).use { fos ->
            // 1. "fLaC" magic
            fos.write(byteArrayOf(0x66, 0x4C, 0x61, 0x43))

            // 2. STREAMINFO block (type 0, length 34, not last)
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x22))
            fos.write(ByteArray(34))

            // 3. VORBIS_COMMENT block (type 4, isLast=true)
            val vendorStr = "reference libFLAC"
            val vendorBytes = vendorStr.toByteArray(StandardCharsets.UTF_8)
            val lyricTag = "LYRICS=[00:01.00]Embedded Flac Lyric Test"
            val lyricBytes = lyricTag.toByteArray(StandardCharsets.UTF_8)

            val blockDataSize = 4 + vendorBytes.size + 4 + 4 + lyricBytes.size
            // Header: bit 7 = 1 (isLast), bits 0..6 = 4 (blockType 4)
            val b0 = (0x80 or 0x04).toByte()
            val b1 = ((blockDataSize ushr 16) and 0xFF).toByte()
            val b2 = ((blockDataSize ushr 8) and 0xFF).toByte()
            val b3 = (blockDataSize and 0xFF).toByte()
            fos.write(byteArrayOf(b0, b1, b2, b3))

            // Vendor length + bytes
            fos.write(intToLe(vendorBytes.size))
            fos.write(vendorBytes)

            // Comment count = 1
            fos.write(intToLe(1))

            // Comment 0 length + bytes
            fos.write(intToLe(lyricBytes.size))
            fos.write(lyricBytes)
        }

        val extracted = EmbeddedLyricsExtractor.extractLyrics(tempFile.absolutePath)
        assertNotNull(extracted)
        assertEquals("[00:01.00]Embedded Flac Lyric Test", extracted)
    }

    @Test
    fun testRomanizedSubLyricsPairing() {
        // Dual timestamp lines with same timestamp: primary Korean line + romanized line
        val lrc = """
            [00:10.00]너를 사랑해
            [00:10.00]neoreul saranghae
            [00:15.00](영원히 기억할게)
            [00:15.00](yeongwonhi gieokhalge)
            [00:20.00]Single line without translation
        """.trimIndent()

        val doc = LrcParser.parse(lrc, durationMs = 30000L)
        assertEquals(3, doc.lines.size)

        // Line 0: Korean primary + romanized sub
        assertEquals("너를 사랑해", doc.lines[0].text)
        assertEquals(1, doc.lines[0].sub.size)
        assertEquals("neoreul saranghae", doc.lines[0].sub[0])
        assertFalse(doc.lines[0].alignRight)

        // Line 1: Parenthetical backing vocals + romanized sub, both right aligned
        assertEquals("(영원히 기억할게)", doc.lines[1].text)
        assertEquals(1, doc.lines[1].sub.size)
        assertEquals("(yeongwonhi gieokhalge)", doc.lines[1].sub[0])
        assertTrue(doc.lines[1].alignRight)
        assertTrue(doc.lines[1].isBackingVocal)

        // Line 2: Single line
        assertEquals("Single line without translation", doc.lines[2].text)
        assertTrue(doc.lines[2].sub.isEmpty())
        assertFalse(doc.lines[2].alignRight)
    }

    @Test
    fun testStandaloneParenthesesDetection() {
        assertTrue(LrcParser.isStandaloneParentheses("(Backing vocal)"))
        assertTrue(LrcParser.isStandaloneParentheses("（全角かっこ）"))
        assertTrue(LrcParser.isStandaloneParentheses("\"(Quoted backing)\""))
        assertTrue(LrcParser.isStandaloneParentheses("(Whispering melodies...)"))

        assertFalse(LrcParser.isStandaloneParentheses("Hello (world)"))
        assertFalse(LrcParser.isStandaloneParentheses("(Start) but not end"))
        assertFalse(LrcParser.isStandaloneParentheses("Regular lyrics line"))
    }

    private fun intToLe(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte()
        )
    }
}

package com.retro.ipod.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing an indexed audio track in the local music library.
 * Fully indexed on title, artist, and album for high-speed drill-down navigation and search.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val filePath: String,
    val codec: String,         // "FLAC", "MP3", "WAV", "ALAC", "AAC", "OGG"
    val sampleRateKhz: Double, // 44.1, 48.0, 96.0, 192.0
    val bitDepth: Int          // 16, 24, 32
)

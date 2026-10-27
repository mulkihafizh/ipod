package com.retro.ipod.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a local user-curated playlist (including "On-The-Go").
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

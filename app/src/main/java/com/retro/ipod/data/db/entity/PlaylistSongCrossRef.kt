package com.retro.ipod.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference entity representing the Many-to-Many relationship
 * between [PlaylistEntity] and [SongEntity] with ordered playlist positioning.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id"]),
        Index(value = ["playlistId", "orderIndex"])
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val id: Long,
    val orderIndex: Int
)

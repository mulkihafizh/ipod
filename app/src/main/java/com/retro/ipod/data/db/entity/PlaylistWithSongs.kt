package com.retro.ipod.data.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Composite model representing a playlist together with its associated songs.
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "id"
        )
    )
    val songs: List<SongEntity>
)

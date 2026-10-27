package com.retro.ipod.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.retro.ipod.data.db.entity.PlaylistEntity
import com.retro.ipod.data.db.entity.PlaylistSongCrossRef
import com.retro.ipod.data.db.entity.PlaylistWithSongs
import com.retro.ipod.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local and "On-The-Go" playlists.
 */
@Dao
interface PlaylistDao {

    /**
     * Observes all user playlists ordered by creation date descending.
     */
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /**
     * Observes a specific playlist by ID.
     */
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId LIMIT 1")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    /**
     * Creates a new playlist and returns its generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    /**
     * Deletes a playlist (cascades cross references).
     */
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    /**
     * Deletes a playlist by ID.
     */
    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    /**
     * Adds a track to a playlist with specified order index.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    /**
     * Removes a track from a playlist.
     */
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * Retrieves ordered list of songs within a specific playlist.
     */
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref xref ON s.id = xref.id
        WHERE xref.playlistId = :playlistId
        ORDER BY xref.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    /**
     * Retrieves a playlist bundled with its songs in a single transaction.
     */
    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    /**
     * Returns the number of songs currently in the playlist.
     */
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getSongCountInPlaylist(playlistId: Long): Int

    /**
     * Direct snapshot of all playlists.
     */
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsDirect(): List<PlaylistEntity>
}

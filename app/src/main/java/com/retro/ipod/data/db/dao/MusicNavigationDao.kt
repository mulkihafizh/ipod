package com.retro.ipod.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retro.ipod.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object powering the iPod hierarchical drill-down navigation:
 * Artists -> Albums -> Songs
 * Albums -> Songs
 * All Songs
 */
@Dao
interface MusicNavigationDao {

    /**
     * Retrieves all unique artist names sorted alphabetically case-insensitively.
     */
    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<String>>

    /**
     * Retrieves all unique albums for a specific artist.
     */
    @Query("SELECT DISTINCT album FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC")
    fun getAlbumsByArtist(artist: String): Flow<List<String>>

    /**
     * Retrieves all songs for a given artist and album, ordered by disc number, track number, then title.
     */
    @Query("SELECT * FROM songs WHERE artist = :artist AND album = :album ORDER BY discNumber ASC, trackNumber ASC, title ASC")
    fun getSongsByArtistAndAlbum(artist: String, album: String): Flow<List<SongEntity>>

    /**
     * Retrieves all unique album names across the entire music library.
     */
    @Query("SELECT DISTINCT album FROM songs ORDER BY album COLLATE NOCASE ASC")
    fun getAllAlbums(): Flow<List<String>>

    /**
     * Retrieves all songs in a specific album, ordered by disc number, track number, then title.
     */
    @Query("SELECT * FROM songs WHERE album = :album ORDER BY discNumber ASC, trackNumber ASC, title ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    /**
     * Retrieves all songs in the library sorted alphabetically case-insensitively.
     */
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    /**
     * Retrieves all songs for a given artist (across all albums).
     */
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC, title ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    /**
     * Observes a specific song by its unique MediaStore ID.
     */
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    fun getSongById(id: Long): Flow<SongEntity?>

    /**
     * Synchronously/directly fetches a song by ID (suspend).
     */
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongByIdDirect(id: Long): SongEntity?

    /**
     * One-shot snapshot of all songs in the library (useful for background search indexing).
     */
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongsSnapshot(): List<SongEntity>

    /**
     * Observes total number of tracks currently indexed in the database.
     */
    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    /**
     * Bulk inserts or replaces audio track entities.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    /**
     * Inserts or replaces a single audio track entity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    /**
     * Updates an existing track entity.
     */
    @Update
    suspend fun updateSong(song: SongEntity)

    /**
     * Deletes a specific track entity.
     */
    @Delete
    suspend fun deleteSong(song: SongEntity)

    /**
     * Clears all indexed songs.
     */
    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
}

package com.retro.ipod.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.retro.ipod.data.db.dao.MusicNavigationDao
import com.retro.ipod.data.db.dao.PlaylistDao
import com.retro.ipod.data.db.entity.PlaylistEntity
import com.retro.ipod.data.db.entity.PlaylistSongCrossRef
import com.retro.ipod.data.db.entity.SongEntity

/**
 * Main Room Database for the Retro iPod Player.
 * Holds songs catalog with search indices and user playlists.
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IpodDatabase : RoomDatabase() {

    abstract fun musicNavigationDao(): MusicNavigationDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val DATABASE_NAME = "retro_ipod.db"

        @Volatile
        private var INSTANCE: IpodDatabase? = null

        fun getInstance(context: Context): IpodDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IpodDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

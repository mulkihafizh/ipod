package com.retro.ipod.playback

import androidx.media3.common.Player
import com.retro.ipod.data.db.entity.SongEntity

/**
 * Immutable snapshot of the current audio playback state for UI layers.
 */
data class CurrentPlaybackState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<SongEntity> = emptyList(),
    val queueIndex: Int = -1,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0L)
}

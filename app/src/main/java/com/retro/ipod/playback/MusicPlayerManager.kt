package com.retro.ipod.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.retro.ipod.data.db.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Central audio playback coordinator connecting Jetpack Compose UI to the background
 * [MusicPlaybackService] via AndroidX Media3 [MediaController].
 *
 * Exposes high-precision 16ms clock ticks for 60fps syllable lyrics synchronization
 * and real-time playback state flows.
 */
class MusicPlayerManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "MusicPlayerManager"
        private const val CLOCK_TICK_INTERVAL_MS = 16L // ~60 FPS
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(CurrentPlaybackState())
    val playbackState: StateFlow<CurrentPlaybackState> = _playbackState.asStateFlow()

    // Dedicated high-precision position StateFlow (emitted every 16ms when playing)
    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private var clockJob: Job? = null
    private var songLookup = mutableMapOf<String, SongEntity>()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().apply {
            addListener(
                {
                    try {
                        val mediaController = get()
                        controller = mediaController
                        attachPlayerListener(mediaController)
                        syncStateFromPlayer(mediaController)
                        Log.i(TAG, "Connected to MusicPlaybackService MediaController")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error connecting to MediaController", e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    }

    private fun attachPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startClockTicker()
                } else {
                    stopClockTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val duration = if (player.duration > 0) player.duration else 0L

                _playbackState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = duration
                    )
                }

                if (playbackState == Player.STATE_READY) {
                    _playbackPositionMs.value = player.currentPosition
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentSong = mediaItem?.mediaId?.let { songLookup[it] }
                val queueIndex = player.currentMediaItemIndex
                val duration = if (player.duration > 0) player.duration else currentSong?.durationMs ?: 0L

                _playbackState.update {
                    it.copy(
                        currentSong = currentSong,
                        queueIndex = queueIndex,
                        durationMs = duration
                    )
                }
                _playbackPositionMs.value = player.currentPosition
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun syncStateFromPlayer(player: Player) {
        val currentSong = player.currentMediaItem?.mediaId?.let { songLookup[it] }
        val isPlaying = player.isPlaying
        val duration = if (player.duration > 0) player.duration else currentSong?.durationMs ?: 0L

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                isPlaying = isPlaying,
                positionMs = player.currentPosition,
                durationMs = duration,
                queueIndex = player.currentMediaItemIndex,
                shuffleMode = player.shuffleModeEnabled,
                repeatMode = player.repeatMode
            )
        }
        _playbackPositionMs.value = player.currentPosition

        if (isPlaying) {
            startClockTicker()
        }
    }

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = scope.launch {
            while (isActive) {
                val p = controller
                if (p != null && p.isPlaying) {
                    val pos = p.currentPosition
                    _playbackPositionMs.value = pos
                    _playbackState.update { it.copy(positionMs = pos) }
                }
                delay(CLOCK_TICK_INTERVAL_MS)
            }
        }
    }

    private fun stopClockTicker() {
        clockJob?.cancel()
        clockJob = null
        controller?.let {
            val pos = it.currentPosition
            _playbackPositionMs.value = pos
            _playbackState.update { s -> s.copy(positionMs = pos) }
        }
    }

    // ==========================================
    // Public Playback Control API
    // ==========================================

    /**
     * Plays a specific song immediately. If a queue is supplied, sets the full queue
     * with playback starting at the chosen track.
     */
    fun playSong(song: SongEntity, newQueue: List<SongEntity> = listOf(song)) {
        val c = controller ?: run {
            Log.w(TAG, "Controller not ready yet, queuing pending track")
            return
        }

        // Cache lookup map for instant metadata retrieval
        songLookup.clear()
        newQueue.forEach { songLookup[it.id.toString()] = it }

        val mediaItems = newQueue.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(Uri.fromFile(File(s.filePath)))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setDisplayTitle(s.title)
                        .build()
                )
                .build()
        }

        val targetIndex = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _playbackState.update {
            it.copy(
                queue = newQueue,
                queueIndex = targetIndex,
                currentSong = song,
                durationMs = song.durationMs
            )
        }

        c.setMediaItems(mediaItems, targetIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            c.play()
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        c.seekTo(positionMs)
        _playbackPositionMs.value = positionMs
        _playbackState.update { it.copy(positionMs = positionMs) }
    }

    fun skipToNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val c = controller ?: return
        // If current position > 3 seconds, replay track (standard iPod behavior)
        if (c.currentPosition > 3000) {
            c.seekTo(0L)
        } else if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
        } else {
            c.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val newShuffle = !c.shuffleModeEnabled
        c.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(shuffleMode = newShuffle) }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        val nextMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        c.repeatMode = nextMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun release() {
        stopClockTicker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}

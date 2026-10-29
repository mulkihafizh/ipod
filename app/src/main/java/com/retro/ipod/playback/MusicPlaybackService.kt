package com.retro.ipod.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.retro.ipod.MainActivity
import com.retro.ipod.R

/**
 * Background Foreground Service managing the ExoPlayer instance and system MediaSession.
 * Handles audio focus ducking, becoming noisy broadcasts (headphone disconnects),
 * and media notification controls including interactive Shuffle and Repeat / Loop toggle buttons.
 */
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_TOGGLE_SHUFFLE = "com.retro.ipod.playback.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "com.retro.ipod.playback.ACTION_CYCLE_REPEAT"
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private fun buildShuffleCommandButton(shuffleEnabled: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (shuffleEnabled) "Shuffle On" else "Shuffle Off")
            .setIconResId(if (shuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
            .setEnabled(true)
            .build()
    }

    private fun buildRepeatCommandButton(repeatMode: Int): CommandButton {
        val (iconRes, displayName) = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one to "Repeat One"
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all to "Repeat All"
            else -> R.drawable.ic_repeat_off to "Repeat Off"
        }
        return CommandButton.Builder()
            .setDisplayName(displayName)
            .setIconResId(iconRes)
            .setSessionCommand(SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY))
            .setEnabled(true)
            .build()
    }

    private fun updateCustomLayout(shuffleEnabled: Boolean, repeatMode: Int) {
        val session = mediaSession ?: return
        val shuffleBtn = buildShuffleCommandButton(shuffleEnabled)
        val repeatBtn = buildRepeatCommandButton(repeatMode)
        session.setCustomLayout(ImmutableList.of(shuffleBtn, repeatBtn))
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Automatic audio focus handling
            .setHandleAudioBecomingNoisy(true)        // Pause when headphones are disconnected
            .setWakeMode(C.WAKE_MODE_LOCAL)           // Keep CPU alive for background audio
            .build()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.accept(
                    availableSessionCommands,
                    connectionResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                val p = player ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_SESSION_DISCONNECTED))
                when (customCommand.customAction) {
                    ACTION_TOGGLE_SHUFFLE -> {
                        val newShuffle = !p.shuffleModeEnabled
                        p.shuffleModeEnabled = newShuffle
                        updateCustomLayout(newShuffle, p.repeatMode)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_CYCLE_REPEAT -> {
                        val nextRepeat = when (p.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        p.repeatMode = nextRepeat
                        updateCustomLayout(p.shuffleModeEnabled, nextRepeat)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> return super.onCustomCommand(session, controller, customCommand, args)
                }
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        // Sync custom layout whenever player's shuffle mode or repeat mode changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                val p = player ?: return
                updateCustomLayout(shuffleModeEnabled, p.repeatMode)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val p = player ?: return
                updateCustomLayout(p.shuffleModeEnabled, repeatMode)
            }
        })

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .build()
        setMediaNotificationProvider(notificationProvider)

        player = exoPlayer
        updateCustomLayout(exoPlayer.shuffleModeEnabled, exoPlayer.repeatMode)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}

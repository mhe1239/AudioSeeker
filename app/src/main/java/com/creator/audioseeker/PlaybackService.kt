package com.creator.audioseeker

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val positionSaver = object : Runnable {
        override fun run() {
            player?.let(::persistPlaybackPosition)
            if (player?.isPlaying == true) {
                handler.postDelayed(this, POSITION_SAVE_INTERVAL_MS)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            player?.let(::persistPlaybackPosition)
            if (isPlaying) startPeriodicPositionSaving() else stopPeriodicPositionSaving()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            player?.let(::persistPlaybackPosition)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            persistPlaybackPosition(oldPosition.mediaItem, oldPosition.positionMs, C.TIME_UNSET)
            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                player?.let(::persistPlaybackPosition)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { trackId ->
                preferences.edit().putLong(KEY_LAST_PLAYED_TRACK_ID, trackId).apply()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(DEFAULT_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(DEFAULT_SEEK_INCREMENT_MS)
            .build()

        player?.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, requireNotNull(player)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        player?.let(::persistPlaybackPosition)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopPeriodicPositionSaving()
        mediaSession?.run {
            player.removeListener(playerListener)
            persistPlaybackPosition(player)
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun startPeriodicPositionSaving() {
        handler.removeCallbacks(positionSaver)
        handler.postDelayed(positionSaver, POSITION_SAVE_INTERVAL_MS)
    }

    private fun stopPeriodicPositionSaving() {
        handler.removeCallbacks(positionSaver)
    }

    private fun persistPlaybackPosition(player: Player) {
        persistPlaybackPosition(player.currentMediaItem, player.currentPosition, player.duration)
    }

    private fun persistPlaybackPosition(mediaItem: MediaItem?, positionMs: Long, durationMs: Long) {
        val trackId = mediaItem?.mediaId?.toLongOrNull() ?: return
        val position = positionMs.coerceAtLeast(0L)
        val editor = preferences.edit()
        if (durationMs != C.TIME_UNSET && durationMs > 0L && position >= durationMs - RESUME_END_THRESHOLD_MS) {
            editor.remove(KEY_PLAYBACK_POSITION_PREFIX + trackId)
        } else {
            editor.putLong(KEY_PLAYBACK_POSITION_PREFIX + trackId, position)
        }
        editor.apply()
    }

    private companion object {
        const val DEFAULT_SEEK_INCREMENT_MS = 10_000L
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
        const val RESUME_END_THRESHOLD_MS = 1_000L
        const val PREFERENCES_NAME = "audioseeker_preferences"
        const val KEY_LAST_PLAYED_TRACK_ID = "last_played_track_id"
        const val KEY_PLAYBACK_POSITION_PREFIX = "playback_position_"
    }
}

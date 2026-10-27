package com.creator.audioseeker

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
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
    private val sleepTimerRunnable = Runnable { finishSleepTimer() }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in SLEEP_TIMER_PREFERENCE_KEYS) handler.post(::scheduleSleepTimer)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            player?.let(::persistPlaybackPosition)
            if (isPlaying) startPeriodicPositionSaving() else stopPeriodicPositionSaving()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            player?.let(::persistPlaybackPosition)
            if (playbackState == Player.STATE_ENDED && isCurrentTrackEndTimer(player?.currentMediaItem)) {
                handler.post(::finishSleepTimer)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && isCurrentTrackEndTimer(oldPosition.mediaItem)) {
                handler.post(::finishSleepTimer)
            }
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
            .setSeekParameters(SeekParameters.EXACT)
            .build()

        player?.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, requireNotNull(player)).build()
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        scheduleSleepTimer()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        player?.let { persistPlaybackPosition(it, synchronously = true) }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopPeriodicPositionSaving()
        handler.removeCallbacks(sleepTimerRunnable)
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        mediaSession?.run {
            player.removeListener(playerListener)
            persistPlaybackPosition(player, synchronously = true)
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

    private fun scheduleSleepTimer() {
        handler.removeCallbacks(sleepTimerRunnable)
        when (readSleepTimerOption()) {
            SleepTimerOption.FIFTEEN_MINUTES,
            SleepTimerOption.THIRTY_MINUTES,
            SleepTimerOption.CUSTOM,
            -> {
                val remainingMs = preferences.getLong(KEY_SLEEP_TIMER_END_TIME_MS, 0L) - System.currentTimeMillis()
                if (remainingMs <= 0L) finishSleepTimer() else handler.postDelayed(sleepTimerRunnable, remainingMs)
            }

            SleepTimerOption.OFF,
            SleepTimerOption.CURRENT_TRACK_END,
            -> Unit
        }
    }

    private fun isCurrentTrackEndTimer(mediaItem: MediaItem?): Boolean =
        readSleepTimerOption() == SleepTimerOption.CURRENT_TRACK_END &&
            mediaItem?.mediaId?.toLongOrNull() == preferences.getLong(KEY_SLEEP_TIMER_TRACK_ID, NO_TRACK_ID)

    private fun finishSleepTimer() {
        handler.removeCallbacks(sleepTimerRunnable)
        player?.pause()
        preferences.edit()
            .putString(KEY_SLEEP_TIMER_OPTION, SleepTimerOption.OFF.name)
            .putLong(KEY_SLEEP_TIMER_END_TIME_MS, 0L)
            .putLong(KEY_SLEEP_TIMER_TRACK_ID, NO_TRACK_ID)
            .apply()
    }

    private fun readSleepTimerOption(): SleepTimerOption =
        preferences.getString(KEY_SLEEP_TIMER_OPTION, null)
            ?.let { value -> SleepTimerOption.entries.firstOrNull { it.name == value } }
            ?: SleepTimerOption.OFF

    private fun persistPlaybackPosition(player: Player, synchronously: Boolean = false) {
        persistPlaybackPosition(
            mediaItem = player.currentMediaItem,
            positionMs = player.currentPosition,
            durationMs = player.duration,
            synchronously = synchronously,
        )
    }

    private fun persistPlaybackPosition(
        mediaItem: MediaItem?,
        positionMs: Long,
        durationMs: Long,
        synchronously: Boolean = false,
    ) {
        val trackId = mediaItem?.mediaId?.toLongOrNull() ?: return
        val position = positionMs.coerceAtLeast(0L)
        val editor = preferences.edit()
        if (durationMs != C.TIME_UNSET && durationMs > 0L && position >= durationMs - RESUME_END_THRESHOLD_MS) {
            editor.remove(KEY_PLAYBACK_POSITION_PREFIX + trackId)
        } else {
            editor.putLong(KEY_PLAYBACK_POSITION_PREFIX + trackId, position)
        }
        if (synchronously) editor.commit() else editor.apply()
    }

    private companion object {
        const val DEFAULT_SEEK_INCREMENT_MS = 10_000L
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
        const val RESUME_END_THRESHOLD_MS = 1_000L
        const val PREFERENCES_NAME = "audioseeker_preferences"
        const val KEY_LAST_PLAYED_TRACK_ID = "last_played_track_id"
        const val KEY_PLAYBACK_POSITION_PREFIX = "playback_position_"
        const val KEY_SLEEP_TIMER_OPTION = "sleep_timer_option"
        const val KEY_SLEEP_TIMER_END_TIME_MS = "sleep_timer_end_time_ms"
        const val KEY_SLEEP_TIMER_TRACK_ID = "sleep_timer_track_id"
        const val NO_TRACK_ID = -1L
        val SLEEP_TIMER_PREFERENCE_KEYS = setOf(
            KEY_SLEEP_TIMER_OPTION,
            KEY_SLEEP_TIMER_END_TIME_MS,
            KEY_SLEEP_TIMER_TRACK_ID,
        )
    }
}

package com.creator.audioseeker

import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val preferences = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val controllerFuture = MediaController.Builder(
        app,
        SessionToken(app, ComponentName(app, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private var pendingDeletion: Uri? = null

    var tracks: List<AudioTrack> by mutableStateOf(emptyList())
        private set

    var playerState by mutableStateOf(PlayerUiState(
        skipSeconds = preferences.getInt(KEY_SKIP_SECONDS, DEFAULT_SKIP_SECONDS),
        controlsLocked = preferences.getBoolean(KEY_CONTROLS_LOCKED, false),
    ))
        private set

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayerState()
        }
    }

    private val libraryObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadTracks()
        }
    }

    init {
        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()?.also {
                    it.addListener(playerListener)
                }
                syncPlayerState()
            },
            ContextCompat.getMainExecutor(app),
        )
        app.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            libraryObserver,
        )
    }

    fun loadTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedTracks = queryTracks(app.contentResolver)
            withContext(Dispatchers.Main) {
                tracks = loadedTracks
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val mediaController = controller ?: return
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return

        val items = tracks.map { audioTrack ->
            MediaItem.Builder()
                .setMediaId(audioTrack.id.toString())
                .setUri(audioTrack.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audioTrack.title)
                        .setArtist(audioTrack.artist)
                        .build(),
                )
                .build()
        }
        mediaController.setMediaItems(items, index, 0L)
        mediaController.prepare()
        mediaController.play()
    }

    fun togglePlayback() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        if (!playerState.controlsLocked) {
            controller?.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    fun seekBy(seconds: Int) {
        if (!playerState.controlsLocked) {
            controller?.seekTo((controller?.currentPosition ?: 0L) + seconds * 1_000L)
        }
    }

    fun skipToPrevious() {
        if (!playerState.controlsLocked) controller?.seekToPreviousMediaItem()
    }

    fun skipToNext() {
        if (!playerState.controlsLocked) controller?.seekToNextMediaItem()
    }

    fun setSkipSeconds(seconds: Int) {
        if (seconds !in SKIP_INTERVALS) return
        preferences.edit().putInt(KEY_SKIP_SECONDS, seconds).apply()
        playerState = playerState.copy(skipSeconds = seconds)
    }

    fun setControlsLocked(locked: Boolean) {
        preferences.edit().putBoolean(KEY_CONTROLS_LOCKED, locked).apply()
        playerState = playerState.copy(controlsLocked = locked)
    }

    fun updateProgress() = syncPlayerState()

    fun createDeleteRequest(track: AudioTrack): IntentSender {
        pendingDeletion = track.uri
        return MediaStore.createDeleteRequest(app.contentResolver, listOf(track.uri)).intentSender
    }

    fun onDeleteResult(deleted: Boolean) {
        pendingDeletion = null
        if (deleted) loadTracks()
    }

    private fun syncPlayerState() {
        val mediaController = controller
        if (mediaController == null) {
            playerState = playerState.copy(isConnected = false)
            return
        }

        val currentTrackId = mediaController.currentMediaItem?.mediaId?.toLongOrNull()
        val duration = mediaController.duration.let { if (it == C.TIME_UNSET) 0L else it.coerceAtLeast(0L) }
        playerState = playerState.copy(
            isConnected = true,
            isPlaying = mediaController.isPlaying,
            currentTrackId = currentTrackId,
            currentPositionMs = mediaController.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
        )
    }

    override fun onCleared() {
        app.contentResolver.unregisterContentObserver(libraryObserver)
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }

    private fun queryTracks(contentResolver: ContentResolver): List<AudioTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return buildList {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        AudioTrack(
                            id = id,
                            uri = Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id.toString(),
                            ),
                            title = cursor.getString(titleColumn).orEmpty().ifBlank { "이름 없는 오디오" },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "알 수 없는 아티스트" },
                            durationMs = cursor.getLong(durationColumn),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_SKIP_SECONDS = 10
        val SKIP_INTERVALS = setOf(3, 5, 10, 15, 30)

        private const val PREFERENCES_NAME = "audioseeker_preferences"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_CONTROLS_LOCKED = "controls_locked"
    }
}

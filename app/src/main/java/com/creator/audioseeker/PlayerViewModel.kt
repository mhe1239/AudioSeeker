package com.creator.audioseeker

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val preferences = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val controllerFuture = MediaController.Builder(
        app,
        SessionToken(app, ComponentName(app, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private var pendingDeletion: Uri? = null
    private var shortcutPlaybackRequested = false
    private var shortcutTrackLoadInProgress = false
    private val savedCurrentTrackId = preferences.getLong(KEY_LAST_PLAYED_TRACK_ID, NO_TRACK_ID)
        .takeIf { it != NO_TRACK_ID }

    var tracks: List<AudioTrack> by mutableStateOf(emptyList())
        private set

    var classificationFolders: List<ClassificationFolder> by mutableStateOf(loadClassificationFolders())
        private set

    var autoClassificationExtensions: Set<String> by mutableStateOf(
        preferences.getStringSet(KEY_AUTO_CLASSIFICATION_EXTENSIONS, DEFAULT_AUTO_CLASSIFICATION_EXTENSIONS)
            ?.map { it.uppercase() }
            ?.toSet()
            ?: DEFAULT_AUTO_CLASSIFICATION_EXTENSIONS,
    )
        private set

    var playerState by mutableStateOf(PlayerUiState(
        currentTrackId = savedCurrentTrackId,
        currentPositionMs = savedCurrentTrackId?.let(::savedPlaybackPositionMs) ?: 0L,
        skipSeconds = preferences.getInt(KEY_SKIP_SECONDS, DEFAULT_SKIP_SECONDS),
        playbackSpeed = preferences.getFloat(KEY_PLAYBACK_SPEED, DEFAULT_PLAYBACK_SPEED)
            .coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED),
        shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false),
        repeatMode = preferences.getString(KEY_REPEAT_MODE, null)
            ?.let { savedValue -> PlaybackRepeatMode.entries.firstOrNull { it.name == savedValue } }
            ?: PlaybackRepeatMode.PLAYLIST_ONCE,
        controlsLocked = preferences.getBoolean(KEY_CONTROLS_LOCKED, false),
        unlockMethod = preferences.getString(KEY_UNLOCK_METHOD, null)
            ?.let { savedValue -> UnlockMethod.entries.firstOrNull { it.name == savedValue } }
            ?: UnlockMethod.DOUBLE_TAP,
    ))
        private set

    var themePreference: ThemePreference by mutableStateOf(
        preferences.getString(KEY_THEME_PREFERENCE, null)
            ?.let { savedValue -> ThemePreference.entries.firstOrNull { it.name == savedValue } }
            ?: ThemePreference.SYSTEM,
    )
        private set

    var showLockScreenArtwork: Boolean by mutableStateOf(
        preferences.getBoolean(KEY_SHOW_LOCK_SCREEN_ARTWORK, true),
    )
        private set

    private val appArtworkData: ByteArray? by lazy { createAppArtworkData() }
    private val embeddedArtworkCache = mutableMapOf<Long, ByteArray>()
    private val artworkCheckedTrackIds = mutableSetOf<Long>()

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
                    it.playbackParameters = PlaybackParameters(playerState.playbackSpeed)
                    it.shuffleModeEnabled = playerState.shuffleEnabled
                    it.repeatMode = playerState.repeatMode.toMedia3RepeatMode()
                }
                syncPlayerState()
                startShortcutPlaybackIfReady()
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
                classifyNewTracks(loadedTracks)
                startShortcutPlaybackIfReady()
            }
        }
    }

    fun playTrack(track: AudioTrack, playlist: List<AudioTrack>) {
        if (playerState.controlsLocked) return
        val mediaController = controller ?: return
        val queue = playlist.distinctBy { it.id }
        val index = queue.indexOfFirst { it.id == track.id }
        if (index < 0) return

        persistCurrentPosition()
        val items = queue.map(::createMediaItem)
        mediaController.shuffleModeEnabled = playerState.shuffleEnabled
        mediaController.repeatMode = playerState.repeatMode.toMedia3RepeatMode()
        mediaController.setMediaItems(items, index, savedPlaybackPositionMs(track.id))
        mediaController.prepare()
        mediaController.play()
        preferences.edit().putLong(KEY_LAST_PLAYED_TRACK_ID, track.id).apply()
        requestEmbeddedArtwork(track)
    }

    fun playLastTrack() {
        if (playerState.controlsLocked) return
        shortcutPlaybackRequested = true
        startShortcutPlaybackIfReady()
    }

    fun togglePlayback() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        if (!playerState.controlsLocked) {
            val targetPositionMs = positionMs.coerceAtLeast(0L)
            if (targetPositionMs == 0L) {
                controller?.currentMediaItem?.mediaId?.toLongOrNull()?.let(::clearSavedPlaybackPosition)
            }
            controller?.seekTo(targetPositionMs)
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

    fun setPlaybackSpeed(speed: Float) {
        val selectedSpeed = PLAYBACK_SPEEDS.minByOrNull { candidate ->
            kotlin.math.abs(candidate - speed)
        } ?: return
        if (kotlin.math.abs(selectedSpeed - speed) > PLAYBACK_SPEED_EPSILON) return

        preferences.edit().putFloat(KEY_PLAYBACK_SPEED, selectedSpeed).apply()
        controller?.playbackParameters = PlaybackParameters(selectedSpeed)
        playerState = playerState.copy(playbackSpeed = selectedSpeed)
    }

    fun toggleShuffle() {
        if (playerState.controlsLocked) return
        val enabled = !playerState.shuffleEnabled
        preferences.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply()
        controller?.shuffleModeEnabled = enabled
        playerState = playerState.copy(shuffleEnabled = enabled)
    }

    fun cycleRepeatMode() {
        if (playerState.controlsLocked) return
        val mode = when (playerState.repeatMode) {
            PlaybackRepeatMode.PLAYLIST_ONCE -> PlaybackRepeatMode.PLAYLIST_REPEAT
            PlaybackRepeatMode.PLAYLIST_REPEAT -> PlaybackRepeatMode.TRACK_REPEAT
            PlaybackRepeatMode.TRACK_REPEAT -> PlaybackRepeatMode.PLAYLIST_ONCE
        }
        preferences.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
        controller?.repeatMode = mode.toMedia3RepeatMode()
        playerState = playerState.copy(repeatMode = mode)
    }

    fun setControlsLocked(locked: Boolean) {
        preferences.edit().putBoolean(KEY_CONTROLS_LOCKED, locked).apply()
        playerState = playerState.copy(controlsLocked = locked)
    }

    fun setUnlockMethod(method: UnlockMethod) {
        preferences.edit().putString(KEY_UNLOCK_METHOD, method.name).apply()
        playerState = playerState.copy(unlockMethod = method)
    }

    fun updateThemePreference(preference: ThemePreference) {
        preferences.edit().putString(KEY_THEME_PREFERENCE, preference.name).apply()
        themePreference = preference
    }

    fun updateShowLockScreenArtwork(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_LOCK_SCREEN_ARTWORK, show).apply()
        showLockScreenArtwork = show

        val mediaController = controller ?: return
        val currentTrack = tracks.firstOrNull { it.id == mediaController.currentMediaItem?.mediaId?.toLongOrNull() }
            ?: return
        val currentIndex = mediaController.currentMediaItemIndex
        if (currentIndex != C.INDEX_UNSET) {
            mediaController.replaceMediaItem(currentIndex, createMediaItem(currentTrack))
            requestEmbeddedArtwork(currentTrack)
        }
    }

    fun addClassificationFolder(name: String, keyword: String?) {
        val trimmedName = name.trim()
        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedName.isBlank()) return

        val filesAtCreation = tracks.map { it.id }.toSet()
        classificationFolders = classificationFolders + ClassificationFolder(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            keyword = trimmedKeyword,
            trackIds = trimmedKeyword?.let { filter ->
                tracks.filter { it.matchesClassificationKeyword(filter) }.map { it.id }.toSet()
            } ?: emptySet(),
            examinedTrackIds = filesAtCreation,
            isInitialized = true,
        )
        saveClassificationFolders()
    }

    fun removeClassificationFolder(folder: ClassificationFolder) {
        classificationFolders = classificationFolders.filterNot { it.id == folder.id }
        saveClassificationFolders()
    }

    fun updateAutoClassificationExtensions(extensions: Set<String>) {
        val normalized = extensions.map { it.uppercase() }.toSet()
        preferences.edit().putStringSet(KEY_AUTO_CLASSIFICATION_EXTENSIONS, normalized).apply()
        autoClassificationExtensions = normalized
    }

    fun updateProgress() = syncPlayerState()

    fun persistCurrentPosition() {
        val mediaController = controller ?: return
        persistPlaybackPosition(
            trackId = mediaController.currentMediaItem?.mediaId?.toLongOrNull(),
            positionMs = mediaController.currentPosition,
            durationMs = mediaController.duration,
            force = true,
        )
    }

    fun createDeleteRequest(tracks: List<AudioTrack>): IntentSender {
        require(tracks.isNotEmpty())
        pendingDeletion = tracks.first().uri
        return MediaStore.createDeleteRequest(app.contentResolver, tracks.map { it.uri }).intentSender
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
        currentTrackId?.let { id ->
            if (showLockScreenArtwork) tracks.firstOrNull { it.id == id }?.let(::requestEmbeddedArtwork)
        }
        val duration = mediaController.duration.let { if (it == C.TIME_UNSET) 0L else it.coerceAtLeast(0L) }
        val reportedPosition = mediaController.currentPosition.coerceAtLeast(0L)
        val shouldKeepSavedPosition =
            mediaController.playbackState != Player.STATE_READY &&
                currentTrackId != null &&
                currentTrackId == playerState.currentTrackId &&
                playerState.currentPositionMs > 0L
        val position = if (shouldKeepSavedPosition) playerState.currentPositionMs else reportedPosition
        persistPlaybackPosition(currentTrackId, position, duration)
        playerState = playerState.copy(
            isConnected = true,
            isPlaying = mediaController.isPlaying,
            currentTrackId = currentTrackId,
            currentPositionMs = position,
            durationMs = duration,
            playbackSpeed = mediaController.playbackParameters.speed,
            shuffleEnabled = mediaController.shuffleModeEnabled,
            repeatMode = mediaController.repeatMode.toPlaybackRepeatMode(),
        )
    }

    private fun startShortcutPlaybackIfReady() {
        if (!shortcutPlaybackRequested || playerState.controlsLocked || controller == null) return

        if (tracks.isEmpty()) {
            if (!shortcutTrackLoadInProgress) {
                shortcutTrackLoadInProgress = true
                viewModelScope.launch(Dispatchers.IO) {
                    val loadedTracks = queryTracks(app.contentResolver)
                    withContext(Dispatchers.Main) {
                        tracks = loadedTracks
                        classifyNewTracks(loadedTracks)
                        shortcutTrackLoadInProgress = false
                        startShortcutPlaybackIfReady()
                    }
                }
            }
            return
        }

        val lastPlayedTrackId = preferences.getLong(KEY_LAST_PLAYED_TRACK_ID, NO_TRACK_ID)
        val track = tracks.firstOrNull { it.id == lastPlayedTrackId } ?: tracks.first()
        shortcutPlaybackRequested = false
        playTrack(track, tracks)
    }

    private fun createMediaItem(track: AudioTrack, artworkData: ByteArray? = null): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .apply {
                    if (showLockScreenArtwork) {
                        (artworkData ?: embeddedArtworkData(track) ?: appArtworkData)
                            ?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
                    }
                }
                .build(),
        )
        .build()

    private fun requestEmbeddedArtwork(track: AudioTrack) {
        if (!showLockScreenArtwork) return
        val shouldLoad = synchronized(artworkCheckedTrackIds) {
            artworkCheckedTrackIds.add(track.id)
        }
        if (!shouldLoad) return

        viewModelScope.launch(Dispatchers.IO) {
            val artworkData = extractEmbeddedArtwork(track.uri) ?: return@launch
            synchronized(embeddedArtworkCache) { embeddedArtworkCache[track.id] = artworkData }
            withContext(Dispatchers.Main) {
                val mediaController = controller ?: return@withContext
                if (showLockScreenArtwork && mediaController.currentMediaItem?.mediaId == track.id.toString()) {
                    val index = mediaController.currentMediaItemIndex
                    if (index != C.INDEX_UNSET) mediaController.replaceMediaItem(index, createMediaItem(track, artworkData))
                }
            }
        }
    }

    private fun embeddedArtworkData(track: AudioTrack): ByteArray? =
        synchronized(embeddedArtworkCache) { embeddedArtworkCache[track.id] }

    private fun extractEmbeddedArtwork(uri: Uri): ByteArray? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(app, uri)
            retriever.embeddedPicture?.let(::resizeArtworkData)
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun createAppArtworkData(): ByteArray? = runCatching {
        BitmapFactory.decodeResource(app.resources, R.drawable.audioseeker_fallback_artwork)
            .let(::compressArtwork)
    }.getOrNull()

    private fun resizeArtworkData(data: ByteArray): ByteArray? = runCatching {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.let(::compressArtwork)
    }.getOrNull()

    private fun compressArtwork(source: Bitmap): ByteArray {
        val largestSide = maxOf(source.width, source.height)
        val artwork = if (largestSide > LOCK_SCREEN_ARTWORK_SIZE) {
            val scale = LOCK_SCREEN_ARTWORK_SIZE.toFloat() / largestSide
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            source
        }
        val compressed = ByteArrayOutputStream().use { output ->
            artwork.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        if (artwork !== source) artwork.recycle()
        source.recycle()
        return compressed
    }

    override fun onCleared() {
        persistCurrentPosition()
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
            MediaStore.Audio.Media.RELATIVE_PATH,
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
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

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
                            relativePath = cursor.getString(relativePathColumn).orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    private fun loadClassificationFolders(): List<ClassificationFolder> = runCatching {
        val savedRules = preferences.getString(KEY_CLASSIFICATION_FOLDERS, "[]") ?: "[]"
        val jsonArray = JSONArray(savedRules)
        buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val keyword = item.optString("keyword").trim().takeIf { it.isNotBlank() }
                if (id.isNotBlank() && name.isNotBlank()) {
                    add(
                        ClassificationFolder(
                            id = id,
                            name = name,
                            keyword = keyword,
                            trackIds = item.optLongSet("trackIds"),
                            examinedTrackIds = item.optLongSet("examinedTrackIds"),
                            isInitialized = item.optBoolean("isInitialized", false),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun saveClassificationFolders() {
        val savedRules = JSONArray().apply {
            classificationFolders.forEach { folder ->
                put(
                    JSONObject().apply {
                        put("id", folder.id)
                        put("name", folder.name)
                        put("keyword", folder.keyword)
                        put("trackIds", JSONArray(folder.trackIds.sorted()))
                        put("examinedTrackIds", JSONArray(folder.examinedTrackIds.sorted()))
                        put("isInitialized", folder.isInitialized)
                    },
                )
            }
        }
        preferences.edit().putString(KEY_CLASSIFICATION_FOLDERS, savedRules.toString()).apply()
    }

    private fun classifyNewTracks(loadedTracks: List<AudioTrack>) {
        if (classificationFolders.isEmpty()) return

        val updatedFolders = classificationFolders.map { folder ->
            val keyword = folder.keyword ?: return@map folder
            val tracksToCheck = if (folder.isInitialized) {
                loadedTracks.filter { it.id !in folder.examinedTrackIds }
            } else {
                // Folders saved by an earlier app version receive one initial scan on upgrade.
                loadedTracks
            }
            if (tracksToCheck.isEmpty() && folder.isInitialized) return@map folder

            folder.copy(
                trackIds = folder.trackIds + tracksToCheck
                    .filter { it.matchesClassificationKeyword(keyword) }
                    .map { it.id },
                examinedTrackIds = folder.examinedTrackIds + tracksToCheck.map { it.id },
                isInitialized = true,
            )
        }
        if (updatedFolders != classificationFolders) {
            classificationFolders = updatedFolders
            saveClassificationFolders()
        }
    }

    private fun AudioTrack.matchesClassificationKeyword(keyword: String): Boolean =
        fileExtension() in autoClassificationExtensions && title.contains(keyword, ignoreCase = true)

    private fun savedPlaybackPositionMs(trackId: Long): Long =
        preferences.getLong(KEY_PLAYBACK_POSITION_PREFIX + trackId, 0L).coerceAtLeast(0L)

    private fun persistPlaybackPosition(
        trackId: Long?,
        positionMs: Long,
        durationMs: Long,
        force: Boolean = false,
    ) {
        val id = trackId ?: return
        val position = positionMs.coerceAtLeast(0L)
        if (durationMs != C.TIME_UNSET && durationMs > 0L && position >= durationMs - RESUME_END_THRESHOLD_MS) {
            clearSavedPlaybackPosition(id)
            return
        }
        if (position == 0L && !force) return

        val savedPosition = savedPlaybackPositionMs(id)
        if (!force && kotlin.math.abs(position - savedPosition) < POSITION_SAVE_INTERVAL_MS) return

        preferences.edit().putLong(KEY_PLAYBACK_POSITION_PREFIX + id, position).apply()
    }

    private fun clearSavedPlaybackPosition(trackId: Long) {
        preferences.edit().remove(KEY_PLAYBACK_POSITION_PREFIX + trackId).apply()
    }

    private fun PlaybackRepeatMode.toMedia3RepeatMode(): Int = when (this) {
        PlaybackRepeatMode.PLAYLIST_ONCE -> Player.REPEAT_MODE_OFF
        PlaybackRepeatMode.PLAYLIST_REPEAT -> Player.REPEAT_MODE_ALL
        PlaybackRepeatMode.TRACK_REPEAT -> Player.REPEAT_MODE_ONE
    }

    private fun Int.toPlaybackRepeatMode(): PlaybackRepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.PLAYLIST_REPEAT
        Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.TRACK_REPEAT
        else -> PlaybackRepeatMode.PLAYLIST_ONCE
    }

    private fun AudioTrack.fileExtension(): String = title.substringAfterLast('.', "").uppercase()

    private fun JSONObject.optLongSet(key: String): Set<Long> {
        val values = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until values.length()) {
                values.optLong(index, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(::add)
            }
        }
    }

    companion object {
        const val DEFAULT_SKIP_SECONDS = 10
        const val DEFAULT_PLAYBACK_SPEED = 1f
        private const val LOCK_SCREEN_ARTWORK_SIZE = 512
        private const val MIN_PLAYBACK_SPEED = 0.5f
        private const val MAX_PLAYBACK_SPEED = 2f
        private const val PLAYBACK_SPEED_EPSILON = 0.001f
        val SKIP_INTERVALS = setOf(3, 5, 10, 15, 30)
        val PLAYBACK_SPEEDS = (5..20).map { it / 10f }
        val SUPPORTED_AUTO_CLASSIFICATION_EXTENSIONS = listOf(
            "MP3", "M4A", "WAV", "FLAC", "AAC", "OGG", "OPUS", "WMA",
        )
        val DEFAULT_AUTO_CLASSIFICATION_EXTENSIONS = SUPPORTED_AUTO_CLASSIFICATION_EXTENSIONS.toSet()

        private const val PREFERENCES_NAME = "audioseeker_preferences"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_PLAYBACK_POSITION_PREFIX = "playback_position_"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_LAST_PLAYED_TRACK_ID = "last_played_track_id"
        private const val KEY_CONTROLS_LOCKED = "controls_locked"
        private const val KEY_UNLOCK_METHOD = "unlock_method"
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        private const val KEY_SHOW_LOCK_SCREEN_ARTWORK = "show_lock_screen_artwork"
        private const val KEY_CLASSIFICATION_FOLDERS = "classification_folders"
        private const val KEY_AUTO_CLASSIFICATION_EXTENSIONS = "auto_classification_extensions"
        private const val NO_TRACK_ID = -1L
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
        private const val RESUME_END_THRESHOLD_MS = 1_000L
    }
}

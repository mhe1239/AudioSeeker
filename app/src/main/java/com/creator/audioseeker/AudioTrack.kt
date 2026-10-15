package com.creator.audioseeker

import android.net.Uri

enum class UnlockMethod {
    DOUBLE_TAP,
    LONG_PRESS,
}

enum class ThemePreference {
    SYSTEM,
    DARK,
    STANDARD_DARK,
    LIGHT,
}

data class ClassificationFolder(
    val id: String,
    val name: String,
    val keyword: String? = null,
    val trackIds: Set<Long> = emptySet(),
    val examinedTrackIds: Set<Long> = emptySet(),
    val isInitialized: Boolean = false,
)

data class AudioTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val relativePath: String,
)

data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrackId: Long? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val skipSeconds: Int = 10,
    val playbackSpeed: Float = 1f,
    val controlsLocked: Boolean = false,
    val unlockMethod: UnlockMethod = UnlockMethod.DOUBLE_TAP,
)

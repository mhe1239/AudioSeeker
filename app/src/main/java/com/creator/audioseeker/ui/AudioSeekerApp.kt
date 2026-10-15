package com.creator.audioseeker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.creator.audioseeker.AudioTrack
import com.creator.audioseeker.PlayerUiState
import com.creator.audioseeker.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun AudioSeekerApp(
    hasAudioPermission: Boolean,
    tracks: List<AudioTrack>,
    playerState: PlayerUiState,
    onRequestAudioPermission: () -> Unit,
    onPlayTrack: (AudioTrack) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSetSkipSeconds: (Int) -> Unit,
    onSetControlsLocked: (Boolean) -> Unit,
    onDeleteTrack: (AudioTrack) -> Unit,
    onRefresh: () -> Unit,
    onUpdateProgress: () -> Unit,
) {
    var settingsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            delay(500)
            onUpdateProgress()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (!hasAudioPermission) {
            AudioPermissionScreen(onRequestAudioPermission)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompactHeight = maxHeight < 420.dp
                val currentTrack = tracks.firstOrNull { it.id == playerState.currentTrackId }

                Column(modifier = Modifier.fillMaxSize()) {
                    AppHeader(
                        onRefresh = onRefresh,
                        onSettings = { settingsOpen = true },
                    )

                    if (tracks.isEmpty()) {
                        EmptyLibrary(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(items = tracks, key = { it.id }) { track ->
                                TrackRow(
                                    track = track,
                                    isCurrent = track.id == playerState.currentTrackId,
                                    onPlay = { onPlayTrack(track) },
                                    onDelete = { onDeleteTrack(track) },
                                )
                            }
                        }
                    }

                    PlayerPanel(
                        track = currentTrack,
                        playerState = playerState,
                        compact = isCompactHeight,
                        onTogglePlayback = onTogglePlayback,
                        onSeekTo = onSeekTo,
                        onSeekBy = onSeekBy,
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext,
                        onToggleLock = { onSetControlsLocked(!playerState.controlsLocked) },
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            selectedSeconds = playerState.skipSeconds,
            onSetSkipSeconds = onSetSkipSeconds,
            onDismiss = { settingsOpen = false },
        )
    }
}

@Composable
private fun AudioPermissionScreen(onRequestAudioPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text("오디오 파일 접근이 필요합니다", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "기기에 있는 음악과 녹음 파일을 목록에 표시하려면 권한을 허용해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestAudioPermission) {
            Text("오디오 파일 허용")
        }
    }
}

@Composable
private fun AppHeader(onRefresh: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("AudioSeeker", style = MaterialTheme.typography.headlineSmall)
            Text(
                "기기의 음악과 녹음",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "파일 목록 새로고침")
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "재생 설정")
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("표시할 오디오 파일이 없습니다", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "새 파일을 추가하면 자동으로 목록에 표시됩니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artist} · ${formatDuration(track.durationMs)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "${track.title} 삭제")
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun PlayerPanel(
    track: AudioTrack?,
    playerState: PlayerUiState,
    compact: Boolean,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLock: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track?.title ?: "재생할 파일을 선택하세요",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!compact) {
                        Text(
                            track?.artist ?: "목록에서 파일을 누르면 재생됩니다",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LockControl(locked = playerState.controlsLocked, onToggleLock = onToggleLock)
            }

            if (playerState.controlsLocked) {
                Text(
                    "조작 잠김 · 잠금 아이콘을 길게 눌러 해제",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val duration = playerState.durationMs.coerceAtLeast(1L)
            Slider(
                value = playerState.currentPositionMs.coerceIn(0L, duration).toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                enabled = track != null && !playerState.controlsLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "재생 위치" },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatDuration(playerState.currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatDuration(playerState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlayerButton(
                    icon = Icons.Default.SkipPrevious,
                    description = "이전 곡",
                    enabled = track != null && !playerState.controlsLocked,
                    onClick = onSkipPrevious,
                )
                PlayerButton(
                    icon = Icons.Default.FastRewind,
                    description = "${playerState.skipSeconds}초 뒤로",
                    enabled = track != null && !playerState.controlsLocked,
                    onClick = { onSeekBy(-playerState.skipSeconds) },
                )
                PlayerButton(
                    icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = if (playerState.isPlaying) "일시정지" else "재생",
                    enabled = track != null,
                    prominent = true,
                    onClick = onTogglePlayback,
                )
                PlayerButton(
                    icon = Icons.Default.FastForward,
                    description = "${playerState.skipSeconds}초 앞으로",
                    enabled = track != null && !playerState.controlsLocked,
                    onClick = { onSeekBy(playerState.skipSeconds) },
                )
                PlayerButton(
                    icon = Icons.Default.SkipNext,
                    description = "다음 곡",
                    enabled = track != null && !playerState.controlsLocked,
                    onClick = onSkipNext,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LockControl(locked: Boolean, onToggleLock: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .combinedClickable(
                onClickLabel = if (locked) "길게 눌러 조작 잠금 해제" else "조작 잠금",
                onLongClickLabel = "조작 잠금 해제",
                role = Role.Button,
                onClick = { if (!locked) onToggleLock() },
                onLongClick = { if (locked) onToggleLock() },
            )
            .semantics {
                contentDescription = if (locked) {
                    "조작 잠김. 길게 눌러 잠금 해제"
                } else {
                    "조작 잠금"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            tint = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(if (prominent) 56.dp else 48.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(if (prominent) 32.dp else 26.dp),
            tint = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsDialog(
    selectedSeconds: Int,
    onSetSkipSeconds: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("재생 설정") },
        text = {
            Column {
                Text("앞·뒤 탐색 시간", style = MaterialTheme.typography.titleSmall)
                PlayerViewModel.SKIP_INTERVALS.sorted().forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetSkipSeconds(seconds) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedSeconds == seconds,
                            onClick = { onSetSkipSeconds(seconds) },
                        )
                        Text("${seconds}초", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("완료") }
        },
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

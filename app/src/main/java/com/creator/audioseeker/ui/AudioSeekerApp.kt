package com.creator.audioseeker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.creator.audioseeker.AudioTrack
import com.creator.audioseeker.ClassificationFolder
import com.creator.audioseeker.PlayerUiState
import com.creator.audioseeker.PlayerViewModel
import com.creator.audioseeker.PlaybackRepeatMode
import com.creator.audioseeker.R
import com.creator.audioseeker.ThemePreference
import com.creator.audioseeker.UnlockMethod
import kotlinx.coroutines.delay

private enum class FolderViewMode {
    DEVICE,
    AUTOMATIC,
}

@Composable
fun AudioSeekerApp(
    hasAudioPermission: Boolean,
    tracks: List<AudioTrack>,
    playerState: PlayerUiState,
    onRequestAudioPermission: () -> Unit,
    onPlayTrack: (AudioTrack, List<AudioTrack>) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSetSkipSeconds: (Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onSetControlsLocked: (Boolean) -> Unit,
    onSetUnlockMethod: (UnlockMethod) -> Unit,
    themePreference: ThemePreference,
    onSetThemePreference: (ThemePreference) -> Unit,
    showLockScreenArtwork: Boolean,
    onSetShowLockScreenArtwork: (Boolean) -> Unit,
    classificationFolders: List<ClassificationFolder>,
    autoClassificationExtensions: Set<String>,
    onAddClassificationFolder: (String, String?) -> Unit,
    onRemoveClassificationFolder: (ClassificationFolder) -> Unit,
    onSetAutoClassificationExtensions: (Set<String>) -> Unit,
    onDeleteTracks: (List<AudioTrack>) -> Unit,
    onRefresh: () -> Unit,
    onUpdateProgress: () -> Unit,
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var folderViewMode by remember { mutableStateOf<FolderViewMode?>(null) }
    var selectedDeviceFolder by remember { mutableStateOf<String?>(null) }
    var selectedClassificationFolder by remember { mutableStateOf<ClassificationFolder?>(null) }
    var classificationEditorOpen by remember { mutableStateOf(false) }
    var pendingDeletionTracks by remember { mutableStateOf<List<AudioTrack>?>(null) }
    var focusOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            delay(500)
            onUpdateProgress()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!hasAudioPermission) {
            AudioPermissionScreen(onRequestAudioPermission)
        } else {
            val currentTrack = tracks.firstOrNull { it.id == playerState.currentTrackId }
            if (focusOpen && currentTrack != null) {
                FocusedPlayerScreen(
                    track = currentTrack,
                    playerState = playerState,
                    onBack = { focusOpen = false },
                    onTogglePlayback = onTogglePlayback,
                    onSeekTo = onSeekTo,
                    onSeekBy = onSeekBy,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onToggleLock = { onSetControlsLocked(!playerState.controlsLocked) },
                )
            } else if (settingsOpen) {
                SettingsScreen(
                    currentTrack = currentTrack,
                    playerState = playerState,
                    selectedThemePreference = themePreference,
                    onSetThemePreference = onSetThemePreference,
                    showLockScreenArtwork = showLockScreenArtwork,
                    onSetShowLockScreenArtwork = onSetShowLockScreenArtwork,
                    selectedSeconds = playerState.skipSeconds,
                    onSetSkipSeconds = onSetSkipSeconds,
                    selectedPlaybackSpeed = playerState.playbackSpeed,
                    onSetPlaybackSpeed = onSetPlaybackSpeed,
                    selectedUnlockMethod = playerState.unlockMethod,
                    onSetUnlockMethod = onSetUnlockMethod,
                    selectedAutoClassificationExtensions = autoClassificationExtensions,
                    onSetAutoClassificationExtensions = onSetAutoClassificationExtensions,
                    onBack = { settingsOpen = false },
                    onOpenFocus = { focusOpen = true },
                    onTogglePlayback = onTogglePlayback,
                    onSeekTo = onSeekTo,
                    onSeekBy = onSeekBy,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onToggleLock = { onSetControlsLocked(!playerState.controlsLocked) },
                )
            } else {
                LibraryScreen(
                    tracks = tracks,
                    currentTrack = currentTrack,
                    playerState = playerState,
                    searchOpen = searchOpen,
                    searchQuery = searchQuery,
                    folderViewMode = folderViewMode,
                    selectedDeviceFolder = selectedDeviceFolder,
                    selectedClassificationFolder = selectedClassificationFolder,
                    classificationFolders = classificationFolders,
                    autoClassificationExtensions = autoClassificationExtensions,
                    onSetSearchOpen = { searchOpen = it },
                    onSearchQueryChange = { searchQuery = it },
                    onSetFolderViewMode = { folderViewMode = it },
                    onSelectDeviceFolder = { folder ->
                        selectedDeviceFolder = folder
                        selectedClassificationFolder = null
                        folderViewMode = null
                    },
                    onSelectClassificationFolder = { folder ->
                        selectedClassificationFolder = folder
                        selectedDeviceFolder = null
                        folderViewMode = null
                    },
                    onClearDeviceFolder = { selectedDeviceFolder = null },
                    onClearClassificationFolder = { selectedClassificationFolder = null },
                    onOpenClassificationEditor = { classificationEditorOpen = true },
                    onRemoveClassificationFolder = { folder ->
                        onRemoveClassificationFolder(folder)
                        if (selectedClassificationFolder?.id == folder.id) {
                            selectedClassificationFolder = null
                        }
                    },
                    onPlayTrack = onPlayTrack,
                    onRequestDeleteTrack = { track -> pendingDeletionTracks = listOf(track) },
                    onRefresh = onRefresh,
                    onSettings = { settingsOpen = true },
                    onOpenFocus = { focusOpen = true },
                    onTogglePlayback = onTogglePlayback,
                    onSeekTo = onSeekTo,
                    onSeekBy = onSeekBy,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onToggleLock = { onSetControlsLocked(!playerState.controlsLocked) },
                )
            }
        }
    }

    if (classificationEditorOpen) {
        ClassificationFolderDialog(
            selectedExtensions = autoClassificationExtensions,
            onAdd = { name, keyword ->
                onAddClassificationFolder(name, keyword)
                classificationEditorOpen = false
            },
            onDismiss = { classificationEditorOpen = false },
        )
    }

    pendingDeletionTracks?.let { tracksToDelete ->
        DeleteConfirmationDialog(
            tracks = tracksToDelete,
            onDismiss = { pendingDeletionTracks = null },
            onConfirm = {
                pendingDeletionTracks = null
                onDeleteTracks(tracksToDelete)
            },
        )
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<AudioTrack>, currentTrack: AudioTrack?, playerState: PlayerUiState,
    searchOpen: Boolean, searchQuery: String, folderViewMode: FolderViewMode?,
    selectedDeviceFolder: String?, selectedClassificationFolder: ClassificationFolder?,
    classificationFolders: List<ClassificationFolder>, autoClassificationExtensions: Set<String>,
    onSetSearchOpen: (Boolean) -> Unit, onSearchQueryChange: (String) -> Unit,
    onSetFolderViewMode: (FolderViewMode?) -> Unit,
    onSelectDeviceFolder: (String) -> Unit,
    onSelectClassificationFolder: (ClassificationFolder) -> Unit,
    onClearDeviceFolder: () -> Unit,
    onClearClassificationFolder: () -> Unit,
    onOpenClassificationEditor: () -> Unit,
    onRemoveClassificationFolder: (ClassificationFolder) -> Unit,
    onPlayTrack: (AudioTrack, List<AudioTrack>) -> Unit, onRequestDeleteTrack: (AudioTrack) -> Unit,
    onRefresh: () -> Unit, onSettings: () -> Unit, onOpenFocus: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit, onToggleLock: () -> Unit,
) {
    val searchResults = remember(tracks, searchQuery) {
        tracks.filter { track ->
            searchQuery.isBlank() || listOf(track.title, track.artist, track.folderName()).any {
                it.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val activeClassificationFolder = classificationFolders.firstOrNull { it.id == selectedClassificationFolder?.id }
        ?: selectedClassificationFolder
    val displayedTracks = remember(searchResults, selectedDeviceFolder, activeClassificationFolder) {
        searchResults.filter { track ->
            (selectedDeviceFolder == null || track.folderName() == selectedDeviceFolder) &&
                (activeClassificationFolder == null || track.id in activeClassificationFolder.trackIds)
        }
    }
    val folders = remember(searchResults) {
        searchResults.groupBy { it.folderName() }.toList().sortedBy { it.first.lowercase() }
    }
    val headerSubtitle = when {
        selectedDeviceFolder != null -> "기기 폴더: $selectedDeviceFolder"
        selectedClassificationFolder != null -> "${if (selectedClassificationFolder.keyword == null) "일반" else "자동 분류"}: ${selectedClassificationFolder.name}"
        folderViewMode == FolderViewMode.DEVICE -> "기기 폴더별 음악과 녹음"
        folderViewMode == FolderViewMode.AUTOMATIC -> "자동 분류 폴더"
        else -> "기기의 음악과 녹음"
    }
    val showBackToAll = folderViewMode != null || selectedDeviceFolder != null || selectedClassificationFolder != null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 420.dp
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppHeader(
                folderViewMode = folderViewMode,
                searchOpen = searchOpen,
                subtitle = headerSubtitle,
                showBackToAll = showBackToAll,
                onBackToAll = {
                    onSetFolderViewMode(null)
                    onClearDeviceFolder()
                    onClearClassificationFolder()
                },
                onRefresh = onRefresh,
                onSearch = { onSetSearchOpen(!searchOpen) },
                onDeviceFolders = {
                    onSetFolderViewMode(if (folderViewMode == FolderViewMode.DEVICE) null else FolderViewMode.DEVICE)
                },
                onAutomaticFolders = {
                    onSetFolderViewMode(if (folderViewMode == FolderViewMode.AUTOMATIC) null else FolderViewMode.AUTOMATIC)
                },
                onSettings = onSettings,
            )
            if (searchOpen) SearchField(searchQuery, onSearchQueryChange) {
                onSearchQueryChange(""); onSetSearchOpen(false)
            }
            if (selectedDeviceFolder != null && folderViewMode == null) {
                SelectedDeviceFolderRow(selectedDeviceFolder, onClearDeviceFolder)
            }
            if (selectedClassificationFolder != null && folderViewMode == null) {
                SelectedClassificationFolderRow(selectedClassificationFolder, onClearClassificationFolder)
            }

            when {
                tracks.isEmpty() -> EmptyLibrary(Modifier.weight(1f))
                folderViewMode == FolderViewMode.DEVICE -> DeviceFolderList(folders, Modifier.weight(1f), onSelectDeviceFolder)
                folderViewMode == FolderViewMode.AUTOMATIC -> ClassificationFolderList(
                    folders = classificationFolders,
                    tracks = tracks,
                    modifier = Modifier.weight(1f),
                    onAddFolder = onOpenClassificationEditor,
                    onSelectFolder = onSelectClassificationFolder,
                    onRemoveFolder = onRemoveClassificationFolder,
                )
                displayedTracks.isEmpty() -> EmptySearchResult(
                    Modifier.weight(1f),
                    searchQuery,
                    selectedDeviceFolder,
                    selectedClassificationFolder,
                )
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(displayedTracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == playerState.currentTrackId,
                            selectionEnabled = !playerState.controlsLocked,
                            onPlay = { onPlayTrack(track, displayedTracks) },
                            onDelete = { onRequestDeleteTrack(track) },
                        )
                    }
                }
            }
            PlayerPanel(
                track = currentTrack, playerState = playerState, compact = isCompactHeight, onOpenFocus = onOpenFocus,
                onTogglePlayback = onTogglePlayback, onSeekTo = onSeekTo, onSeekBy = onSeekBy,
                onSkipPrevious = onSkipPrevious, onSkipNext = onSkipNext,
                onToggleShuffle = onToggleShuffle, onCycleRepeatMode = onCycleRepeatMode,
                onToggleLock = onToggleLock,
            )
        }
    }
}

@Composable
private fun AudioPermissionScreen(onRequestAudioPermission: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("오디오 파일 접근이 필요합니다", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("기기에 있는 음악과 녹음 파일을 목록에 표시하려면 권한을 허용해 주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestAudioPermission) { Text("오디오 파일 허용") }
    }
}

@Composable
private fun AppHeader(
    folderViewMode: FolderViewMode?, searchOpen: Boolean, subtitle: String,
    showBackToAll: Boolean, onBackToAll: () -> Unit, onRefresh: () -> Unit,
    onSearch: () -> Unit, onDeviceFolders: () -> Unit, onAutomaticFolders: () -> Unit, onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackToAll) {
            HelpIconButton("전체 음악으로 돌아가기", onBackToAll, Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                "AudioSeeker",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HelpIconButton(
            label = if (searchOpen) "검색 닫기" else "음악 검색",
            onClick = onSearch,
            modifier = Modifier.size(48.dp),
        ) { Icon(if (searchOpen) Icons.Default.Close else Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) }
        HelpIconButton(
            if (folderViewMode == FolderViewMode.DEVICE) "곡 목록 보기" else "기기 폴더 보기",
            onDeviceFolders,
            Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.Folder,
                null,
                tint = if (folderViewMode == FolderViewMode.DEVICE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        HelpIconButton(
            if (folderViewMode == FolderViewMode.AUTOMATIC) "곡 목록 보기" else "자동 분류 폴더 보기",
            onAutomaticFolders,
            Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.FilterAlt,
                null,
                tint = if (folderViewMode == FolderViewMode.AUTOMATIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        HelpIconButton("파일 목록 새로고침", onRefresh, Modifier.size(48.dp)) {
            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        HelpIconButton("앱 설정", onSettings, Modifier.size(48.dp)) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true, label = { Text("파일명, 크리에이터 또는 폴더 검색") },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = {
            HelpIconButton(
                label = if (query.isEmpty()) "검색 닫기" else "검색어 지우기",
                onClick = { if (query.isEmpty()) onClose() else onQueryChange("") },
            ) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun SelectedDeviceFolderRow(folder: String, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("기기 폴더: $folder", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextButton(onClear) { Text("전체") }
    }
}

@Composable
private fun DeviceFolderList(folders: List<Pair<String, List<AudioTrack>>>, modifier: Modifier, onSelectFolder: (String) -> Unit) {
    LazyColumn(modifier) {
        item { Text("기기 폴더", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium) }
        items(folders, key = { it.first }) { (folder, folderTracks) ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelectFolder(folder) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${folderTracks.size}개 파일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SelectedClassificationFolderRow(folder: ClassificationFolder, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (folder.keyword == null) Icons.Default.Folder else Icons.Default.FilterAlt,
            null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (folder.keyword == null) "일반 폴더: ${folder.name}" else "자동 분류: ${folder.name}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                folder.keyword?.let { "검색어: $it" } ?: "자동 추가 없음",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClear) { Text("전체") }
    }
}

@Composable
private fun ClassificationFolderList(
    folders: List<ClassificationFolder>,
    tracks: List<AudioTrack>,
    modifier: Modifier,
    onAddFolder: () -> Unit,
    onSelectFolder: (ClassificationFolder) -> Unit,
    onRemoveFolder: (ClassificationFolder) -> Unit,
) {
    LazyColumn(modifier) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("자동 분류 폴더", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "필터를 지정하면 현재 파일을 검사하고 이후 새 파일도 자동으로 분류합니다. 필터 없이 만들면 비어 있는 일반 폴더가 됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAddFolder) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("폴더 추가")
                }
            }
        }
        if (folders.isEmpty()) {
            item {
                Text(
                    "아직 만든 폴더가 없습니다.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(folders, key = { it.id }) { folder ->
            val matchedCount = tracks.count { it.id in folder.trackIds }
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelectFolder(folder) }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (folder.keyword == null) Icons.Default.Folder else Icons.Default.FilterAlt,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        folder.keyword?.let { "검색어: $it · ${matchedCount}개 파일" }
                            ?: "일반 폴더 · ${matchedCount}개 파일",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HelpIconButton(
                    "${folder.name} ${if (folder.keyword == null) "일반" else "자동 분류"} 폴더 삭제",
                    { onRemoveFolder(folder) },
                    Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("표시할 오디오 파일이 없습니다", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("새 파일을 추가하면 자동으로 목록에 표시됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptySearchResult(
    modifier: Modifier,
    query: String,
    selectedDeviceFolder: String?,
    selectedClassificationFolder: ClassificationFolder?,
) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("표시할 파일이 없습니다", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        val message = when {
            query.isNotBlank() -> "\"$query\"와 일치하는 음악이나 녹음이 없습니다."
            selectedClassificationFolder != null -> "${selectedClassificationFolder.name} ${if (selectedClassificationFolder.keyword == null) "일반" else "자동 분류"} 폴더에 표시할 파일이 없습니다."
            selectedDeviceFolder != null -> "$selectedDeviceFolder 기기 폴더에 표시할 파일이 없습니다."
            else -> "다른 검색어나 폴더를 선택해 보세요."
        }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeleteConfirmationDialog(
    tracks: List<AudioTrack>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("곡 ${tracks.size}개를 삭제할까요?")
        },
        text = {
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "아래 파일이 기기에서 삭제됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                tracks.forEach { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${track.artist} · ${track.folderName()}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "계속하면 Android 권한 확인 창이 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun TrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    selectionEnabled: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val selectionContentAlpha = if (selectionEnabled || isCurrent) 1f else 0.5f
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = background) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = selectionEnabled, role = Role.Button, onClick = onPlay).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.MusicNote,
                null,
                Modifier.size(32.dp).alpha(selectionContentAlpha),
                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f).alpha(selectionContentAlpha)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = titleColor, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                Text("${track.artist} · ${formatDuration(track.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCurrent) Text("현재 재생 중", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                else Text(track.folderName(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HelpIconButton("${track.title} 삭제", onDelete, Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun PlayerPanel(
    track: AudioTrack?, playerState: PlayerUiState, compact: Boolean, onOpenFocus: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit, onToggleLock: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).clickable(enabled = track != null, role = Role.Button, onClick = onOpenFocus)
                        .semantics { if (track != null) contentDescription = "현재 재생 화면 열기" },
                ) {
                    Text(track?.title ?: "재생할 파일을 선택하세요", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    if (!compact) Text(track?.artist ?: "목록에서 파일을 누르면 재생됩니다", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LockControl(playerState.controlsLocked, playerState.unlockMethod, onToggleLock)
            }
            if (playerState.controlsLocked) LockHint(playerState.unlockMethod)
            PlaybackProgress(track != null, playerState, onSeekTo)
            PlaybackControls(track != null, playerState, onTogglePlayback, onSeekBy, onSkipPrevious, onSkipNext)
            if (!compact) PlaybackModeControls(playerState, onToggleShuffle, onCycleRepeatMode)
        }
    }
}

@Composable
private fun FocusedPlayerScreen(
    track: AudioTrack, playerState: PlayerUiState, onBack: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit, onToggleLock: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            HelpIconButton("라이브러리로 돌아가기", onBack, Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("현재 재생", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            LockControl(playerState.controlsLocked, playerState.unlockMethod, onToggleLock)
        }
        Spacer(Modifier.height(28.dp))
        Card(
            Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Image(
                painter = painterResource(R.drawable.audioseeker_fallback_artwork),
                contentDescription = "기본 앨범 아트",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(track.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(track.folderName(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(20.dp))
        PlaybackProgress(true, playerState, onSeekTo)
        Spacer(Modifier.height(6.dp))
        PlaybackControls(true, playerState, onTogglePlayback, onSeekBy, onSkipPrevious, onSkipNext)
        PlaybackModeControls(playerState, onToggleShuffle, onCycleRepeatMode)
        if (playerState.controlsLocked) {
            LockHint(playerState.unlockMethod)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PlaybackProgress(trackAvailable: Boolean, playerState: PlayerUiState, onSeekTo: (Long) -> Unit) {
    val duration = playerState.durationMs.coerceAtLeast(1L)
    Slider(
        value = playerState.currentPositionMs.coerceIn(0L, duration).toFloat(), onValueChange = { onSeekTo(it.toLong()) },
        valueRange = 0f..duration.toFloat(), enabled = trackAvailable && !playerState.controlsLocked,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "재생 위치" },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.secondary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(formatDuration(playerState.currentPositionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(formatDuration(playerState.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaybackControls(
    trackAvailable: Boolean, playerState: PlayerUiState, onTogglePlayback: () -> Unit,
    onSeekBy: (Int) -> Unit, onSkipPrevious: () -> Unit, onSkipNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PlayerButton(Icons.Default.SkipPrevious, "이전 곡", trackAvailable && !playerState.controlsLocked, onClick = onSkipPrevious)
        PlayerButton(Icons.Default.FastRewind, "${playerState.skipSeconds}초 뒤로", trackAvailable && !playerState.controlsLocked, onClick = { onSeekBy(-playerState.skipSeconds) })
        PlayerButton(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (playerState.isPlaying) "일시정지" else "재생", trackAvailable, true, onTogglePlayback)
        PlayerButton(Icons.Default.FastForward, "${playerState.skipSeconds}초 앞으로", trackAvailable && !playerState.controlsLocked, onClick = { onSeekBy(playerState.skipSeconds) })
        PlayerButton(Icons.Default.SkipNext, "다음 곡", trackAvailable && !playerState.controlsLocked, onClick = onSkipNext)
    }
}

@Composable
private fun PlaybackModeControls(
    playerState: PlayerUiState,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackModeButton(
            icon = Icons.Default.Shuffle,
            label = if (playerState.shuffleEnabled) "셔플 켜짐" else "셔플 꺼짐",
            selected = playerState.shuffleEnabled,
            enabled = !playerState.controlsLocked,
            onClick = onToggleShuffle,
        )
        PlaybackModeButton(
            icon = if (playerState.repeatMode == PlaybackRepeatMode.TRACK_REPEAT) Icons.Default.RepeatOne else Icons.Default.Repeat,
            label = playerState.repeatMode.repeatLabel(),
            selected = playerState.repeatMode != PlaybackRepeatMode.PLAYLIST_ONCE,
            enabled = !playerState.controlsLocked,
            onClick = onCycleRepeatMode,
        )
    }
}

@Composable
private fun PlaybackModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    HelpIconButton(label, onClick, Modifier.heightIn(min = 48.dp), enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                Modifier.size(24.dp),
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LockControl(locked: Boolean, unlockMethod: UnlockMethod, onToggleLock: () -> Unit) {
    Box(
        Modifier.size(48.dp).combinedClickable(
            onClickLabel = if (locked) unlockMethod.unlockActionLabel() else "조작 잠금",
            onLongClickLabel = if (unlockMethod == UnlockMethod.LONG_PRESS) "조작 잠금 해제" else null,
            role = Role.Button,
            onClick = { if (!locked) onToggleLock() },
            onDoubleClick = { if (locked && unlockMethod == UnlockMethod.DOUBLE_TAP) onToggleLock() },
            onLongClick = { if (locked && unlockMethod == UnlockMethod.LONG_PRESS) onToggleLock() },
        ).semantics { contentDescription = if (locked) "조작 잠김. ${unlockMethod.unlockActionLabel()} 해제" else "조작 잠금" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (locked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LockHint(unlockMethod: UnlockMethod) {
    Text(
        "조작 잠김 · 잠금 아이콘을 ${unlockMethod.unlockActionLabel()} 해제",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean,
    prominent: Boolean = false, onClick: () -> Unit,
) {
    val modifier = if (prominent) {
        Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier.size(48.dp)
    }
    HelpIconButton(description, onClick, modifier, enabled) {
        Icon(
            icon,
            null,
            Modifier.size(if (prominent) 32.dp else 26.dp),
            if (prominent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = rememberBelowTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) { Text(label) }
        },
        state = rememberTooltipState(),
        focusable = false,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.semantics { contentDescription = label },
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
private fun rememberBelowTooltipPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density) {
        val edgeMargin = with(density) { 8.dp.roundToPx() }
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val anchorIsOnLeft = anchorBounds.left + anchorBounds.width / 2 < windowSize.width / 2
                val requestedX = if (anchorIsOnLeft) {
                    anchorBounds.left
                } else {
                    anchorBounds.right - popupContentSize.width
                }
                val maxX = (windowSize.width - popupContentSize.width - edgeMargin).coerceAtLeast(edgeMargin)
                val x = requestedX.coerceIn(edgeMargin, maxX)

                val belowY = anchorBounds.bottom + edgeMargin
                val y = if (belowY + popupContentSize.height <= windowSize.height - edgeMargin) {
                    belowY
                } else {
                    (anchorBounds.top - popupContentSize.height - edgeMargin).coerceAtLeast(edgeMargin)
                }
                return IntOffset(x, y)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    currentTrack: AudioTrack?,
    playerState: PlayerUiState,
    selectedSeconds: Int,
    onSetSkipSeconds: (Int) -> Unit,
    selectedPlaybackSpeed: Float,
    onSetPlaybackSpeed: (Float) -> Unit,
    selectedUnlockMethod: UnlockMethod,
    onSetUnlockMethod: (UnlockMethod) -> Unit,
    selectedThemePreference: ThemePreference,
    onSetThemePreference: (ThemePreference) -> Unit,
    showLockScreenArtwork: Boolean,
    onSetShowLockScreenArtwork: (Boolean) -> Unit,
    selectedAutoClassificationExtensions: Set<String>,
    onSetAutoClassificationExtensions: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onOpenFocus: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleLock: () -> Unit,
) {
    BackHandler(onBack = onBack)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 420.dp
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HelpIconButton("라이브러리로 돌아가기", onBack, Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(8.dp))
                Text("앱 설정", style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("화면 모드", style = MaterialTheme.typography.titleSmall)
                ThemePreference.entries.forEach { preference ->
                    Row(Modifier.fillMaxWidth().clickable { onSetThemePreference(preference) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedThemePreference == preference, onClick = { onSetThemePreference(preference) })
                        Text(preference.settingsLabel(), Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("잠금화면 Now Bar 앨범 아트", style = MaterialTheme.typography.titleSmall)
                Text(
                    "켜면 잠금화면과 Now Bar를 열었을 때 곡의 앨범 아트를 표시합니다. 이미지가 없으면 기본 앨범 아트를 표시합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().clickable { onSetShowLockScreenArtwork(!showLockScreenArtwork) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = showLockScreenArtwork,
                        onCheckedChange = onSetShowLockScreenArtwork,
                    )
                    Text("앨범 아트 표시", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("앞·뒤 탐색 시간", style = MaterialTheme.typography.titleSmall)
                PlayerViewModel.SKIP_INTERVALS.sorted().forEach { seconds ->
                    Row(Modifier.fillMaxWidth().clickable { onSetSkipSeconds(seconds) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedSeconds == seconds, onClick = { onSetSkipSeconds(seconds) })
                        Text("${seconds}초", Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("재생 속도", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${formatPlaybackSpeed(selectedPlaybackSpeed)}배",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = selectedPlaybackSpeed,
                    onValueChange = onSetPlaybackSpeed,
                    valueRange = PlayerViewModel.PLAYBACK_SPEEDS.first()..PlayerViewModel.PLAYBACK_SPEEDS.last(),
                    steps = PlayerViewModel.PLAYBACK_SPEEDS.size - 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "재생 속도 ${formatPlaybackSpeed(selectedPlaybackSpeed)}배" },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("0.5배", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("2.0배", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                Text("삼성 루틴 재생", style = MaterialTheme.typography.titleSmall)
                Text(
                    "모드 및 루틴에서 애플리케이션 > 앱을 열거나 앱 동작 바로 실행 > AudioSeeker > 재생을 선택하면 마지막 재생 곡을 시작합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("조작 잠금 해제 방식", style = MaterialTheme.typography.titleSmall)
                UnlockMethod.entries.forEach { method ->
                    Row(Modifier.fillMaxWidth().clickable { onSetUnlockMethod(method) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedUnlockMethod == method, onClick = { onSetUnlockMethod(method) })
                        Text(method.settingsLabel(), Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("자동 분류 파일 형식", style = MaterialTheme.typography.titleSmall)
                Text(
                    "선택한 형식의 새 오디오 파일만 자동 분류 폴더 규칙에 적용됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PlayerViewModel.SUPPORTED_AUTO_CLASSIFICATION_EXTENSIONS.forEach { extension ->
                    val isSelected = extension in selectedAutoClassificationExtensions
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onSetAutoClassificationExtensions(
                                selectedAutoClassificationExtensions.toMutableSet().apply {
                                    if (isSelected) remove(extension) else add(extension)
                                },
                            )
                        }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                onSetAutoClassificationExtensions(
                                    selectedAutoClassificationExtensions.toMutableSet().apply {
                                        if (checked) add(extension) else remove(extension)
                                    },
                                )
                            },
                        )
                        Text(".$extension", Modifier.padding(start = 8.dp))
                    }
                }
            }
            PlayerPanel(
                track = currentTrack,
                playerState = playerState,
                compact = isCompactHeight,
                onOpenFocus = onOpenFocus,
                onTogglePlayback = onTogglePlayback,
                onSeekTo = onSeekTo,
                onSeekBy = onSeekBy,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                onToggleLock = onToggleLock,
            )
        }
    }
}

@Composable
private fun ClassificationFolderDialog(
    selectedExtensions: Set<String>,
    onAdd: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    val isAutomaticFolder = keyword.isNotBlank()
    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("폴더 추가") },
        text = {
            Column {
                Text(
                    "파일을 옮기지 않습니다. 검색어를 입력하면 자동 분류 폴더가 되고, 비워 두면 파일을 자동으로 넣지 않는 일반 폴더가 됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("폴더 이름") },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("파일명 필터 검색어 (선택)") },
                    supportingText = { Text("예: 회의 · 비워 두면 일반 폴더") },
                )
                Spacer(Modifier.height(12.dp))
                if (isAutomaticFolder) {
                    Text(
                        if (selectedExtensions.isEmpty()) {
                            "현재 자동 분류 파일 형식이 선택되지 않았습니다. 설정에서 선택해 주세요."
                        } else {
                            "적용 형식: ${selectedExtensions.sorted().joinToString(separator = ", ") { ".${it.lowercase()}" }}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "일반 폴더에는 파일이 자동으로 추가되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, keyword.takeIf { it.isNotBlank() }) }, enabled = canSave) { Text("추가") }
        },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

private fun UnlockMethod.settingsLabel(): String = when (this) {
    UnlockMethod.DOUBLE_TAP -> "두 번 눌러 해제"
    UnlockMethod.LONG_PRESS -> "길게 눌러 해제"
}

private fun ThemePreference.settingsLabel(): String = when (this) {
    ThemePreference.SYSTEM -> "시스템 설정 사용"
    ThemePreference.DARK -> "청록 다크 모드"
    ThemePreference.STANDARD_DARK -> "표준 다크 모드"
    ThemePreference.LIGHT -> "라이트 모드"
}

private fun PlaybackRepeatMode.repeatLabel(): String = when (this) {
    PlaybackRepeatMode.PLAYLIST_ONCE -> "목록 1회 재생"
    PlaybackRepeatMode.PLAYLIST_REPEAT -> "목록 반복"
    PlaybackRepeatMode.TRACK_REPEAT -> "한 곡 반복"
}

private fun UnlockMethod.unlockActionLabel(): String = when (this) {
    UnlockMethod.DOUBLE_TAP -> "두 번 눌러"
    UnlockMethod.LONG_PRESS -> "길게 눌러"
}

private fun AudioTrack.folderName(): String {
    val path = relativePath.trim().trimEnd('/')
    return path.ifBlank { "기타" }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatPlaybackSpeed(speed: Float): String = "%.1f".format(java.util.Locale.US, speed)

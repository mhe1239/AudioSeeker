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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creator.audioseeker.AudioTrack
import com.creator.audioseeker.ClassificationFolder
import com.creator.audioseeker.PlayerUiState
import com.creator.audioseeker.PlayerViewModel
import com.creator.audioseeker.R
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
    onPlayTrack: (AudioTrack) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSetSkipSeconds: (Int) -> Unit,
    onSetControlsLocked: (Boolean) -> Unit,
    onSetUnlockMethod: (UnlockMethod) -> Unit,
    classificationFolders: List<ClassificationFolder>,
    autoClassificationExtensions: Set<String>,
    onAddClassificationFolder: (String, String) -> Unit,
    onRemoveClassificationFolder: (ClassificationFolder) -> Unit,
    onSetAutoClassificationExtensions: (Set<String>) -> Unit,
    onDeleteTrack: (AudioTrack) -> Unit,
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
                    onDeleteTrack = onDeleteTrack,
                    onRefresh = onRefresh,
                    onSettings = { settingsOpen = true },
                    onOpenFocus = { focusOpen = true },
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

    if (settingsOpen) {
        SettingsDialog(
            selectedSeconds = playerState.skipSeconds,
            onSetSkipSeconds = onSetSkipSeconds,
            selectedUnlockMethod = playerState.unlockMethod,
            onSetUnlockMethod = onSetUnlockMethod,
            selectedAutoClassificationExtensions = autoClassificationExtensions,
            onSetAutoClassificationExtensions = onSetAutoClassificationExtensions,
            onDismiss = { settingsOpen = false },
        )
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
    onPlayTrack: (AudioTrack) -> Unit, onDeleteTrack: (AudioTrack) -> Unit,
    onRefresh: () -> Unit, onSettings: () -> Unit, onOpenFocus: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleLock: () -> Unit,
) {
    val searchResults = remember(tracks, searchQuery) {
        tracks.filter { track ->
            searchQuery.isBlank() || listOf(track.title, track.artist, track.folderName()).any {
                it.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val displayedTracks = remember(searchResults, selectedDeviceFolder, selectedClassificationFolder, autoClassificationExtensions) {
        searchResults.filter { track ->
            (selectedDeviceFolder == null || track.folderName() == selectedDeviceFolder) &&
                (selectedClassificationFolder == null || track.matchesClassificationFolder(selectedClassificationFolder, autoClassificationExtensions))
        }
    }
    val folders = remember(searchResults) {
        searchResults.groupBy { it.folderName() }.toList().sortedBy { it.first.lowercase() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 420.dp
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppHeader(
                folderViewMode = folderViewMode, searchOpen = searchOpen, onRefresh = onRefresh,
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
                    tracks = searchResults,
                    selectedExtensions = autoClassificationExtensions,
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
                            onPlay = { onPlayTrack(track) },
                            onDelete = { onDeleteTrack(track) },
                        )
                    }
                }
            }
            PlayerPanel(
                track = currentTrack, playerState = playerState, compact = isCompactHeight, onOpenFocus = onOpenFocus,
                onTogglePlayback = onTogglePlayback, onSeekTo = onSeekTo, onSeekBy = onSeekBy,
                onSkipPrevious = onSkipPrevious, onSkipNext = onSkipNext, onToggleLock = onToggleLock,
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
    folderViewMode: FolderViewMode?, searchOpen: Boolean, onRefresh: () -> Unit,
    onSearch: () -> Unit, onDeviceFolders: () -> Unit, onAutomaticFolders: () -> Unit, onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("AudioSeeker", style = MaterialTheme.typography.headlineSmall)
            Text(
                when (folderViewMode) {
                    FolderViewMode.DEVICE -> "기기 폴더별 음악과 녹음"
                    FolderViewMode.AUTOMATIC -> "자동 분류 폴더"
                    null -> "기기의 음악과 녹음"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onSearch, Modifier.size(48.dp)) { Icon(if (searchOpen) Icons.Default.Close else Icons.Default.Search, if (searchOpen) "검색 닫기" else "음악 검색") }
        IconButton(onDeviceFolders, Modifier.size(48.dp)) {
            Icon(
                Icons.Default.Folder,
                if (folderViewMode == FolderViewMode.DEVICE) "곡 목록 보기" else "기기 폴더 보기",
                tint = if (folderViewMode == FolderViewMode.DEVICE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onAutomaticFolders, Modifier.size(48.dp)) {
            Icon(
                Icons.Default.FilterAlt,
                if (folderViewMode == FolderViewMode.AUTOMATIC) "곡 목록 보기" else "자동 분류 폴더 보기",
                tint = if (folderViewMode == FolderViewMode.AUTOMATIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onRefresh, Modifier.size(48.dp)) { Icon(Icons.Default.Refresh, "파일 목록 새로고침") }
        IconButton(onSettings, Modifier.size(48.dp)) { Icon(Icons.Default.Settings, "재생 설정") }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true, label = { Text("파일명, 크리에이터 또는 폴더 검색") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            IconButton(onClick = { if (query.isEmpty()) onClose() else onQueryChange("") }) {
                Icon(Icons.Default.Close, if (query.isEmpty()) "검색 닫기" else "검색어 지우기")
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
        Icon(Icons.Default.FilterAlt, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("자동 분류: ${folder.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("검색어: ${folder.keyword}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClear) { Text("전체") }
    }
}

@Composable
private fun ClassificationFolderList(
    folders: List<ClassificationFolder>,
    tracks: List<AudioTrack>,
    selectedExtensions: Set<String>,
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
                    "새 파일이 검색어와 설정된 확장자에 맞으면 자동으로 여기에 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAddFolder) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("분류 폴더 추가")
                }
            }
        }
        if (folders.isEmpty()) {
            item {
                Text(
                    "아직 만든 자동 분류 폴더가 없습니다.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(folders, key = { it.id }) { folder ->
            val matchedCount = tracks.count { it.matchesClassificationFolder(folder, selectedExtensions) }
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelectFolder(folder) }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FilterAlt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("검색어: ${folder.keyword} · ${matchedCount}개 파일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onRemoveFolder(folder) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "${folder.name} 자동 분류 폴더 삭제", tint = MaterialTheme.colorScheme.error)
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
            selectedClassificationFolder != null -> "${selectedClassificationFolder.name} 자동 분류 폴더에 표시할 파일이 없습니다."
            selectedDeviceFolder != null -> "$selectedDeviceFolder 기기 폴더에 표시할 파일이 없습니다."
            else -> "다른 검색어나 폴더를 선택해 보세요."
        }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    selectionEnabled: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = background) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = selectionEnabled, role = Role.Button, onClick = onPlay).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp), if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = titleColor, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                Text("${track.artist} · ${formatDuration(track.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCurrent) Text("현재 재생 중", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                else Text(track.folderName(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onDelete, Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "${track.title} 삭제", tint = MaterialTheme.colorScheme.error) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun PlayerPanel(
    track: AudioTrack?, playerState: PlayerUiState, compact: Boolean, onOpenFocus: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleLock: () -> Unit,
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
        }
    }
}

@Composable
private fun FocusedPlayerScreen(
    track: AudioTrack, playerState: PlayerUiState, onBack: () -> Unit,
    onTogglePlayback: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Int) -> Unit,
    onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onToggleLock: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "라이브러리로 돌아가기") }
            Text("현재 재생", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            LockControl(playerState.controlsLocked, playerState.unlockMethod, onToggleLock)
        }
        Spacer(Modifier.height(28.dp))
        Card(
            Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Image(
                painter = painterResource(R.drawable.audioseeker_brand_icon),
                contentDescription = "AudioSeeker 아이콘",
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentScale = ContentScale.Fit,
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
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
            icon,
            description,
            Modifier.size(if (prominent) 32.dp else 26.dp),
            if (prominent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsDialog(
    selectedSeconds: Int,
    onSetSkipSeconds: (Int) -> Unit,
    selectedUnlockMethod: UnlockMethod,
    onSetUnlockMethod: (UnlockMethod) -> Unit,
    selectedAutoClassificationExtensions: Set<String>,
    onSetAutoClassificationExtensions: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("재생 설정") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text("앞·뒤 탐색 시간", style = MaterialTheme.typography.titleSmall)
                PlayerViewModel.SKIP_INTERVALS.sorted().forEach { seconds ->
                    Row(Modifier.fillMaxWidth().clickable { onSetSkipSeconds(seconds) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedSeconds == seconds, onClick = { onSetSkipSeconds(seconds) })
                        Text("${seconds}초", Modifier.padding(start = 8.dp))
                    }
                }
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
        },
        confirmButton = { TextButton(onDismiss) { Text("완료") } },
    )
}

@Composable
private fun ClassificationFolderDialog(
    selectedExtensions: Set<String>,
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && keyword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("자동 분류 폴더 추가") },
        text = {
            Column {
                Text(
                    "파일을 옮기지 않고, 새로 인식된 파일을 이 규칙에 따라 모아 보여 줍니다.",
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
                    label = { Text("파일명 검색어") },
                    supportingText = { Text("예: 컴파일러구성론") },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (selectedExtensions.isEmpty()) {
                        "현재 자동 분류 파일 형식이 선택되지 않았습니다. 설정에서 선택해 주세요."
                    } else {
                        "적용 형식: ${selectedExtensions.sorted().joinToString(separator = ", ") { ".${it.lowercase()}" }}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, keyword) }, enabled = canSave) { Text("추가") }
        },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

private fun UnlockMethod.settingsLabel(): String = when (this) {
    UnlockMethod.DOUBLE_TAP -> "두 번 눌러 해제"
    UnlockMethod.LONG_PRESS -> "길게 눌러 해제"
}

private fun UnlockMethod.unlockActionLabel(): String = when (this) {
    UnlockMethod.DOUBLE_TAP -> "두 번 눌러"
    UnlockMethod.LONG_PRESS -> "길게 눌러"
}

private fun AudioTrack.folderName(): String {
    val path = relativePath.trim().trimEnd('/')
    return path.ifBlank { "기타" }
}

private fun AudioTrack.matchesClassificationFolder(
    folder: ClassificationFolder,
    selectedExtensions: Set<String>,
): Boolean =
    fileExtension() in selectedExtensions && title.contains(folder.keyword, ignoreCase = true)

private fun AudioTrack.fileExtension(): String = title.substringAfterLast('.', "").uppercase()

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

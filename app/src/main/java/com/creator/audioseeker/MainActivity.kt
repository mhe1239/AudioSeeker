package com.creator.audioseeker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.creator.audioseeker.ui.AudioSeekerApp
import com.creator.audioseeker.ui.theme.AudioSeekerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val playerViewModel: PlayerViewModel = viewModel()
            var hasAudioPermission by remember { mutableStateOf(hasAudioPermission()) }
            val audioPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                hasAudioPermission = granted
                if (granted) playerViewModel.loadTracks()
            }
            val deleteLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                playerViewModel.onDeleteResult(result.resultCode == RESULT_OK)
            }

            LaunchedEffect(hasAudioPermission) {
                if (hasAudioPermission) playerViewModel.loadTracks()
            }

            AudioSeekerTheme(playerViewModel.themePreference) {
                AudioSeekerApp(
                    hasAudioPermission = hasAudioPermission,
                    tracks = playerViewModel.tracks,
                    playerState = playerViewModel.playerState,
                    onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission()) },
                    onPlayTrack = playerViewModel::playTrack,
                    onTogglePlayback = playerViewModel::togglePlayback,
                    onSeekTo = playerViewModel::seekTo,
                    onSeekBy = playerViewModel::seekBy,
                    onSkipPrevious = playerViewModel::skipToPrevious,
                    onSkipNext = playerViewModel::skipToNext,
                    onSetSkipSeconds = playerViewModel::setSkipSeconds,
                    onSetControlsLocked = playerViewModel::setControlsLocked,
                    onSetUnlockMethod = playerViewModel::setUnlockMethod,
                    themePreference = playerViewModel.themePreference,
                    onSetThemePreference = playerViewModel::updateThemePreference,
                    showLockScreenArtwork = playerViewModel.showLockScreenArtwork,
                    onSetShowLockScreenArtwork = playerViewModel::updateShowLockScreenArtwork,
                    classificationFolders = playerViewModel.classificationFolders,
                    autoClassificationExtensions = playerViewModel.autoClassificationExtensions,
                    onAddClassificationFolder = playerViewModel::addClassificationFolder,
                    onRemoveClassificationFolder = playerViewModel::removeClassificationFolder,
                    onSetAutoClassificationExtensions = playerViewModel::updateAutoClassificationExtensions,
                    onDeleteTracks = { tracks ->
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(playerViewModel.createDeleteRequest(tracks)).build(),
                        )
                    },
                    onRefresh = playerViewModel::loadTracks,
                    onUpdateProgress = playerViewModel::updateProgress,
                )
            }
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
}

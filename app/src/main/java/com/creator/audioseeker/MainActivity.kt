package com.creator.audioseeker

import android.Manifest
import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.creator.audioseeker.ui.AudioSeekerApp
import com.creator.audioseeker.ui.theme.AudioSeekerTheme

class MainActivity : ComponentActivity() {
    private var pendingShortcutAction by mutableStateOf<String?>(null)
    private var playerViewModel: PlayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingShortcutAction = intent?.action
        setContent {
            val playerViewModel: PlayerViewModel = viewModel()
            this@MainActivity.playerViewModel = playerViewModel
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

            LaunchedEffect(hasAudioPermission, pendingShortcutAction) {
                if (!hasAudioPermission) return@LaunchedEffect

                if (pendingShortcutAction == ACTION_PLAY_LAST_TRACK) {
                    playerViewModel.playLastTrack()
                    pendingShortcutAction = null
                } else {
                    playerViewModel.loadTracks()
                }
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
                    onSetPlaybackSpeed = playerViewModel::setPlaybackSpeed,
                    reduceBoundarySilence = playerViewModel.reduceBoundarySilence,
                    onSetReduceBoundarySilence = playerViewModel::updateReduceBoundarySilence,
                    sleepTimerOption = playerViewModel.sleepTimerOption,
                    sleepTimerEndTimeMs = playerViewModel.sleepTimerEndTimeMs,
                    customSleepTimerMinutes = playerViewModel.customSleepTimerMinutes,
                    onSetSleepTimer = playerViewModel::setSleepTimer,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onCycleRepeatMode = playerViewModel::cycleRepeatMode,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutAction = intent.action
    }

    override fun onPause() {
        playerViewModel?.persistCurrentPlaybackPosition()
        super.onPause()
    }

    override fun onStop() {
        playerViewModel?.persistCurrentPlaybackPosition()
        super.onStop()
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    companion object {
        const val ACTION_PLAY_LAST_TRACK = "com.creator.audioseeker.action.PLAY_LAST_TRACK"
    }
}

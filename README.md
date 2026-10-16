# AudioSeeker

English | [한국어](./docs/README.ko.md)

<p align="center">
  <img src="assets/branding/audioseeker-icon-solid-teal-transparent.png" width="144" alt="AudioSeeker app icon">
</p>

**AudioSeeker** is an Android player for finding and playing audio stored on your device. It uses the folder or search result you are viewing as the playback queue, with no streaming service, account, or cloud sync in the way.

## Features

- **Local library** — Scans device audio through MediaStore. Search by title, creator, or folder, and sort by name, date added, or duration.
- **Folders and smart folders** — Browse device folders and create in-app smart folders without moving or duplicating your files.
- **Context-aware playback** — Plays the current folder or search result as a queue, with shuffle plus play once, repeat all, and repeat one modes.
- **Playback controls** — Supports previous/next, configurable seek intervals, per-file resume positions, and playback speeds from 0.5x to 2.0x.
- **Transitions and timer** — Optionally trims only contiguous silence at the beginning and end of a file, and offers sleep timers for 15 minutes, 30 minutes, the current track, or a custom duration.
- **Accessible interface** — Includes light, system, teal dark, and standard dark themes; compact-height support; interaction lock; and long-press icon help.
- **Android integration** — Uses a Media3 media session for background playback and system media controls, and provides an app shortcut for a Samsung Modes and Routines playback action.
- **Safe deletion** — Requests deletion only after an in-app confirmation followed by Android's system permission confirmation.

## Requirements

| Component | Version |
| --- | --- |
| Minimum Android version | Android 11 (API 30) |
| Target / compile SDK | API 35 |
| Language and UI | Kotlin, Jetpack Compose, Material 3 |
| Playback engine | AndroidX Media3 |
| Java | 17 |

AudioSeeker needs media access permission to read the local library. Depending on the Android version, it may also request notification permission.

## Build

### Prerequisites

- Android Studio or the Android SDK and Build Tools
- JDK 17
- Windows PowerShell or Command Prompt

### Create a debug APK

```powershell
.\gradlew.bat assembleDebug
```

The APK is created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Project layout

```text
app/
  src/main/java/com/creator/audioseeker/
    PlayerViewModel.kt     # Single source of truth for library and playback state
    PlaybackService.kt     # Media3 playback and media-session service
    ui/                    # Compose screens, components, and theme
  src/main/res/            # Strings, icons, app shortcuts, and styles
docs/                      # Architecture, technical specifications, product plan, and work records
```

See [REQUIREMENTS.md](./REQUIREMENTS.md) for the detailed product requirements and [docs](./docs/) for architecture and technical conventions.

## Verification status

The following flows still need additional regression testing on physical devices:

- Playback-service position persistence and restoration
- Sleep timer behavior during background and repeated playback
- Boundary-silence trimming across audio formats
- Manufacturer-specific Modes and Routines and media-notification visibility

See the [TODO index](./docs/todo/00-todo-list.md) for the tracked work.

## Scope

AudioSeeker is an offline local-audio player. Streaming services, user accounts, cloud sync, playlists, favorites, and A-B repeat are outside the current scope.

# ZYRO

**ZYRO** — Android customization project for a TikTok-compatible experience. The project is being built as modular Android libraries so its own settings UI can be integrated only through a compatible, authorized host-app path.

> ZYRO does not include TikTok's proprietary source, APK, signing keys, credentials, or private service endpoints. The current modules are independent libraries and are not injected into TikTok.

## Current implementation

The repository contains an Android Gradle project with two modules.

### `zyro-settings`

- Jetpack Compose settings screen in Russian.
- Dark ZYRO visual foundation with purple accent.
- Liquid Glass master toggle.
- Blur and transparency toggles (these store preferences; a host must implement the actual visual effects).
- Surface intensity, corner radius, and animation-strength controls.
- System / dark / light appearance mode selection.
- Reduce-motion accessibility option.
- Violet / pink / cyan accent preset selection.
- Reset appearance settings.
- Local persistence with Android DataStore Preferences.

### `zyro-camera`

- Camera2 camera enumeration, with front / rear / external camera labels.
- Camera2 hardware level and sensor orientation.
- Advertised AE FPS ranges.
- Video output resolutions exposed for `MediaRecorder`.
- Constrained high-speed video size / FPS combinations where Android exposes them.
- Electronic and optical stabilization capability flags.
- Dynamic-range profiles (SDR / HLG10 / HDR10 / HDR10+) on Android 13+.
- Compose diagnostics screen with a permission prompt for extended metadata and a refresh action.
- Runs metadata queries on an IO dispatcher.
- Does not open the camera, record audio/video, or take photos.

### Camera diagnostics entry point

Render the screen from a Compose host:

```kotlin
ZyroCameraDiagnosticsScreen(
    onClose = { /* return to the host app */ }
)
```

Add the module to an authorized Android host project:

```kotlin
implementation(project(":zyro-camera"))
```

The library manifest declares `android.permission.CAMERA` and an optional camera hardware feature. Android merges the permission into the host manifest; the host still controls the runtime permission flow. The diagnostics screen asks for permission only when the user chooses extended diagnostics. Basic information is scanned without opening a camera.

Camera2 FPS ranges and high-speed combinations are device-reported capabilities, not guarantees for a particular app's recording pipeline, thermal state, export settings, or server upload. TikTok may process or re-encode media independently.

## Planned settings

### Video and upload quality
- Display capture and export capabilities separately.
- Quality presets based on device capabilities.
- Warn when a selected export setting may be re-encoded by the platform.
- No claims that a setting can force a server-side video format.

### Region and language
- Show app-reported region / language information where available.
- Locale and interface-language preferences.
- Account region, catalog availability, and server-side feature eligibility remain controlled by the platform. ZYRO will not spoof identity, location, or eligibility, or bypass regional restrictions.

### Privacy and quality of life
- Local-only settings storage by default.
- No collection of TikTok passwords, session cookies, or authentication tokens.
- Clear cache controls limited to ZYRO-owned data.
- Diagnostics export with personal data redacted.
- Settings search, favorites, import/export of ZYRO preferences, compatibility labels, changelog, diagnostics, and reset options.
- Russian and English localization.

## Technical constraints

- TikTok is proprietary. A compiled APK is not the original editable Kotlin/Java project.
- The current modules do not modify, inject into, or repackage TikTok.
- Integration must use a compatible, authorized route and respect Android security boundaries and applicable platform terms.
- Do not patch authentication, payment, anti-abuse, integrity, or account-protection mechanisms.
- Camera controls must be based on Android-reported device capabilities; never advertise unsupported FPS modes.
- Blur/transparency controls in the initial UI are stored preferences only. Rendering effects must be supplied by the host integration.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Then build both libraries:

```bash
gradle :zyro-settings:assembleDebug :zyro-camera:assembleDebug
```

No Gradle wrapper is included yet. The source changes have been committed, but no successful build or physical-device test has been confirmed yet.

## Roadmap

1. **Foundation** — project structure, settings model, theme tokens. (Initial slice committed)
2. **Settings UI and persistence** — Compose screen and DataStore. (Initial slice committed)
3. **Camera2 diagnostics** — camera enumeration, supported sizes, frame-rate ranges, stabilization and HDR metadata. (Implemented; build/device verification pending)
4. **FPS selector** — expose only supported 30/60 FPS configurations and validate on real devices.
5. **Liquid Glass rendering** — implement adjustable visual effects with performance and accessibility fallbacks.
6. **Compatibility testing** — Android versions, front/rear cameras, rotation, and low-memory behavior.
7. **Integration assessment** — determine a supported host-app integration path without redistributing or modifying proprietary app code.

# ZYRO

**ZYRO** — Android customization project for a TikTok-compatible experience. The project is being built as a modular Android library so its own settings UI can be integrated only through a compatible, authorized host-app path.

> ZYRO does not include TikTok's proprietary source, APK, signing keys, credentials, or private service endpoints. The current settings module is an independent library and is not injected into TikTok.

## Current implementation

The repository now contains an Android Gradle project and the `zyro-settings` Android library module.

Implemented in this first slice:

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

### Module entry point

The host app can render the settings screen from Compose:

```kotlin
ZyroSettingsScreen(
    onClose = { /* return to the host app's settings */ }
)
```

Add the module to an authorized Android host project and include its dependency:

```kotlin
implementation(project(":zyro-settings"))
```

The library currently uses namespace `app.zyro.settings`, minSdk 23, compileSdk 35, Kotlin 2.0.21, Android Gradle Plugin 8.7.3, and Java 17 bytecode.

## Planned settings

### Camera
- Detect available cameras and query supported output sizes / frame-rate ranges.
- Camera2 compatibility status with a short explanation.
- 30 FPS / 60 FPS selection only when the selected camera and capture configuration support it.
- Auto mode, which selects a supported stable configuration.
- Front / rear camera capability comparison.
- Preview diagnostics: resolution, reported FPS range, stabilization availability, and HDR availability where exposed by Android.
- Graceful fallback to the app's original camera path when an option is unsupported.

### Video and upload quality
- Display capture and export capabilities separately (capture FPS does not guarantee upload FPS).
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
- The current module does not modify, inject into, or repackage TikTok.
- Integration must use a compatible, authorized route and respect Android security boundaries and applicable platform terms.
- Do not patch authentication, payment, anti-abuse, integrity, or account-protection mechanisms.
- Camera controls must be based on Android-reported device capabilities; never advertise unsupported FPS modes.
- Blur/transparency controls in this initial UI are stored preferences only. Rendering effects must be supplied by the host integration.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Then build the library module:

```bash
gradle :zyro-settings:assembleDebug
```

No Gradle wrapper is included yet. The repository changes have not been compiled or device-tested in this environment.

## Roadmap

1. **Foundation** — project structure, settings model, theme tokens. (Initial slice committed)
2. **Settings UI and persistence** — Compose screen and DataStore. (Initial slice committed)
3. **Camera diagnostics** — enumerate cameras and report Camera2 capabilities.
4. **FPS selector** — expose only supported 30/60 FPS configurations and validate on real devices.
5. **Liquid Glass rendering** — implement adjustable visual effects with performance and accessibility fallbacks.
6. **Compatibility testing** — Android versions, front/rear cameras, rotation, lifecycle, and low-memory behavior.
7. **Integration assessment** — determine a supported host-app integration path without redistributing or modifying proprietary app code.

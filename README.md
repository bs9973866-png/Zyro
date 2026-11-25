# ZYRO

**ZYRO** — Android customization project for a TikTok-compatible experience. The goal is to provide a polished settings layer and device-aware camera controls while keeping account security and platform safeguards intact.

> This repository is currently a clean starting point. No TikTok APK, proprietary source code, signing keys, or credentials are included.

## Product vision

- A dedicated **ZYRO Settings** entry reachable from the app's profile/settings area, where integration is supported.
- A premium dark interface with optional Liquid Glass styling.
- Camera capability detection and clear 30/60 FPS choices only when the active camera reports those modes as supported.
- Per-device diagnostics, feature compatibility indicators, and safe fallbacks.
- A modular architecture so unsupported features can be disabled without breaking the base app.

## Planned settings

### Appearance
- Liquid Glass: blur, translucency, surface intensity, corner radius, and animation strength.
- Accent color presets and custom accent.
- Light / dark / system theme.
- Reduce motion and reduce transparency options.
- Restore defaults.

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
- Show the app's reported region / language information where it is available.
- Locale and interface-language preferences.
- Explain that account region, catalog availability, and server-side feature eligibility are controlled by the platform. ZYRO will not spoof identity, location, or eligibility, or bypass regional restrictions.

### Privacy and security
- Local-only settings storage by default.
- No collection of TikTok passwords, session cookies, or authentication tokens.
- Clear cache controls limited to ZYRO-owned data.
- Diagnostics export with personal data redacted.
- Permission and privacy dashboard.

### Quality of life
- Search settings.
- Favorites / pinned settings.
- Import / export ZYRO preferences (no account data).
- Per-feature compatibility labels: Supported, Limited, Unsupported.
- Changelog, diagnostics, and reset options.
- Accessibility: scalable text, contrast controls, reduced motion.
- Russian and English localization.

## Technical direction

- Android-first project.
- Kotlin for Android integration and Camera2 capability inspection.
- Jetpack Compose for ZYRO-owned settings UI, if compatible with the chosen integration approach.
- A feature registry with capability checks and safe fallbacks.
- Automated unit tests for settings, capability parsing, and configuration validation.

## Important constraints

- TikTok is a proprietary third-party application. This repository does not contain or redistribute its APK, proprietary code, assets, or signing material.
- Integration must respect applicable platform terms and Android security boundaries.
- Do not patch authentication, payment, anti-abuse, integrity, or account-protection mechanisms.
- Do not spoof GPS, device identity, account region, or server eligibility.
- Camera controls must be based on Android-reported device capabilities; never advertise unsupported FPS modes.
- A standalone settings prototype can be built before any app integration is attempted.

## Milestones

1. **Foundation** — project structure, branding, settings model, theme tokens.
2. **Settings prototype** — ZYRO settings screen, navigation, search, persistence.
3. **Camera diagnostics** — enumerate cameras and report Camera2 capabilities.
4. **FPS selector** — expose only supported 30/60 FPS configurations and validate on real devices.
5. **Liquid Glass** — implement adjustable visual effects with performance and accessibility fallbacks.
6. **Compatibility testing** — Android versions, front/rear cameras, rotation, lifecycle, and low-memory behavior.
7. **Integration assessment** — determine a supported integration path without redistributing or modifying proprietary app code.

## Status

Planning / initial scaffold. No APK integration or device validation has been completed yet.

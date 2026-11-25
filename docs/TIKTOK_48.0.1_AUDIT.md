# ZYRO — TikTok 48.0.1 package inspection

## Input artifact

- Uploaded file: `TikTok+-+Videos,+Shop+&+LIVE_48.0.1_APKPure.xapk`
- Container size: 472,229,430 bytes
- SHA-256: `18a70c64af65efecdc5681268ec41d747b7573f5b8a8b4f54f46fd3fa4d1105b`
- XAPK manifest package: `com.zhiliaoapp.musically`
- Version name: `48.0.1`
- Version code: `2024800010`
- minSdk: 23
- targetSdk: 36
- Split package includes base APK, ARM64 ABI split, English and mdpi splits, a `df_a_dex` split, and additional feature/configuration splits.

## Confirmed archive structure

The XAPK contains 18 entries:
- Main APK: `com.zhiliaoapp.musically.apk`
- `config.arm64_v8a.apk`
- `config.en.apk`
- `config.mdpi.apk`
- `df_a_dex.apk`
- Kakao, LINE, location, OPPO HyperBoost, and PNS business feature/configuration splits
- `manifest.json` and `icon.png`

The main APK archive contains 23,106 ZIP entries. The `df_a_dex.apk` split contains 31 DEX files (`classes.dex` through `classes31.dex`). The ARM64 split contains native libraries under `lib/arm64-v8a/`.

## What this establishes

1. This is a split XAPK bundle, not one self-contained APK.
2. The package manifest identifies TikTok package name and version 48.0.1.
3. The bundle contains substantial compiled DEX code and native libraries. It does not contain the original Kotlin/Java project or editable source tree.
4. A modified build cannot retain TikTok's original signing identity. Re-signing a modified app changes its signer and may prevent updating or co-installing it as the original package. Do not uninstall the user's working TikTok just to test an experimental build.
5. The filename indicates an APKPure-distributed bundle; this inspection does not establish that the bundle is an authentic, unmodified ByteDance release. Verify provenance and signatures against an official installation before trusting or installing a modified copy.

## Tooling / current limitations

The inspection environment has Java but does not currently have apktool, JADX, Android build-tools (aapt/apkanalyzer), or Androguard installed. Binary AndroidManifest.xml and DEX files have therefore not yet been decompiled or semantically analyzed.

## Safe next engineering steps

- Obtain a trusted, user-authorized copy of the exact installed build and compare package/version/signing certificate information.
- Decode only the app metadata/resources needed to assess compatibility; do not alter authentication, payment, integrity, anti-abuse, or account-protection code.
- Choose a first ZYRO feature with a clear, testable scope.
- Prefer an add-on/integration approach that leaves the original TikTok installation intact. No in-app ZYRO menu or working modified APK has been produced by this inspection.

## Current status

**Package inventory complete; code-level compatibility analysis not yet started.** No APK was modified, repackaged, or signed.

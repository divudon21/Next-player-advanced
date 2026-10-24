# Next Player — Liquid Glass Navigation

A liquid-glass navigation redesign for the open-source **Next Player** Android video player. The app is built with Kotlin and Jetpack Compose and keeps the existing local media, network, playlist, history, and search features.

## Navigation

The floating bottom navigation uses five tabs in this order:

- **Home** — on-device videos
- **Radio** — network media and streams
- **Library** — playlists
- **Explore** — history and app utilities
- **Search** — video and folder search

The glass capsule, animated selection puck, spring motion, icon scaling, press feedback, and scroll-away/reveal animation are implemented in Compose. The visual treatment is inspired by the navigation in [Convx](https://github.com/cosmictaserdev-creator/Convx); the underlying app is based on [Next Player](https://github.com/anilbeesetti/nextplayer). See [NOTICE](NOTICE) and [LICENSE](LICENSE) for attribution and licensing.

## Build locally

Use JDK 17 and the checked-in Gradle wrapper:

```bash
./gradlew :app:assembleReleaseWithDebugSigning
```

The output APKs are written under `app/build/outputs/apk/release-with-debug-signing/`:

- `app-arm64-v8a-release-with-debug-signing.apk`
- `app-armeabi-v7a-release-with-debug-signing.apk`
- `app-universal-release-with-debug-signing.apk`

These APKs are signed with the local Android debug key and use the `.release` application-ID suffix, so they install separately from the official app. Do not use them to update an official Play Store installation.

## GitHub Actions and releases

The **Android APK Build** workflow runs on pushes to `main` and `arena/**` branches, pull requests, and `v*` tags. It builds the three APKs above, runs unit tests and ktlint, and uploads the APKs plus SHA-256 checksums as workflow artifacts. Pushing a `v*` tag also publishes those files as a GitHub Release.

## Upstream project

This repository contains the Next Player Android app from [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer), licensed under GPL-3.0. The liquid-glass navigation styling is an independent Compose adaptation inspired by Convx's GPL-3.0 navigation design.

# Next Player — Convx Navigation Port

A Convx primary-navigation port for the open-source **Next Player** Android video player. The app is built with Kotlin and Jetpack Compose and retains its local media, network, playlist, history, and search features.

## Navigation

The floating bottom navigation uses five tabs in this order:

- **Home** — on-device videos
- **Radio** — network media and streams
- **Library** — playlists
- **Explore** — history and app utilities
- **Search** — video and folder search

The bar is adapted from Convx's actual `AppFloatingNavBar`, `FoldingTabBar`, and `FoldingTabGroup` implementation. It retains Convx's fold-on-scroll behavior, detached Search control, draggable liquid-glass selection puck, capsule shapes, and motion/press interactions. The target app's routes, labels, and navigation state remain native to Next Player; Convx's music-player accessory and inline search-mode branches are not included. See [NOTICE](NOTICE) and [LICENSE](LICENSE) for attribution and licensing.

## Build locally

Use JDK 17 and the checked-in Gradle wrapper:

```bash
./gradlew :app:assembleRelease-with-debug-signing
```

The output APKs are written under `app/build/outputs/apk/release-with-debug-signing/`:

- `app-arm64-v8a-release-with-debug-signing.apk`
- `app-armeabi-v7a-release-with-debug-signing.apk`
- `app-universal-release-with-debug-signing.apk`

These APKs are signed with the local Android debug key and use the `.release` application-ID suffix, so they install separately from the official app. Do not use them to update an official Play Store installation.

## GitHub Actions and releases

The **Android APK Build** workflow runs on pushes to `main` and `arena/**` branches, pull requests, and `v*` tags. It builds the three APKs above, runs unit tests and ktlint, and uploads the APKs plus SHA-256 checksums as workflow artifacts. Pushing a `v*` tag also publishes those files as a GitHub Release.

## Upstream projects

This repository contains the Next Player Android app from [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer), licensed under GPL-3.0. The primary navigation is adapted from [Convx](https://github.com/cosmictaserdev-creator/Convx), also GPL-3.0. The included glass-backdrop engine and capsule-shape sources retain their Apache-2.0 notices; see [NOTICE](NOTICE) and `LICENSES/Apache-2.0.txt`.

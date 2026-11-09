# Veylora integration plan and progress

## Sources and boundaries

- Base: TMPlayer origin/main, b4e93bf9.
- Reference: TorBox-Cinema origin/main-2, 746fa4b.
- Development: feature/torbox-integration-rebrand. No merge, push or release.
- Keep com.tmplayer and existing database, Telegram credentials, playback and storage formats.
- TMPlayer is GPL-3.0-or-later. TorBox-Cinema has no project-wide license in its tracked tree.
  Its code and native binaries must not be copied without permission. Implement against the
  official API, using that repository only to identify supported behavior.

## Integration map

| Area | Existing foundation | Integration |
| --- | --- | --- |
| Network | core HttpTransport, UrlConnectionTransport | TorBox adapter, no extra client |
| Media | MediaItem, MediaName, SeriesShelf | Map cloud files to existing media records |
| Playback | Android PlayerActivity, Media3, NextLib | HTTPS source in the existing player |
| Tracks | Existing audio/subtitle selectors | Embedded tracks, no invented transcoding API |
| Settings | SettingsScreen, SettingsStore | TorBox destination and Keystore credential store |
| Library | Existing filename parsing | Cloud files, title/episode parsing, recent sort |
| Branding | Shared palette, BrandLoader, Android vectors | Original V/play symbol and violet palette |
| Downloads | Telegram-specific DownloadQueue | Do not route HTTPS into TDLib's queue |

## Platform behavior and edge cases

- Android phone and TV: account connection independent of Telegram, explicit refresh, bounded
  library pages, cloud playback through PlayerActivity. TV has overscan padding, initial focus,
  D-pad buttons and Back. Keep API 23 and all existing ABIs.
- Desktop: preserve Telegram and mpv behavior. Shared TorBox API is reusable, but desktop cloud
  navigation and playback require a separate implementation and validation.
- Web: preserve existing player. Do not expose API keys in browser storage or promise cloud
  support without a credential and CORS design.
- Reject blank/control-character keys, failed authentication and non-HTTPS playback URLs.
  Never log credentials or signed stream URLs. Keep credentials out of backup.
- Skip malformed entries, limit response size and page size, stop refresh at end of list.
  Preserve a previous library when refresh fails. No automatic refresh or unbounded retry loop.
- Cancel screen operations when leaving, serialize account changes and refresh, clear the old
  account's library on disconnect. Handle expired links by requesting another on player retry.
- Quality means selecting actual files/versions in the cloud library. No fabricated resolutions.
- Cloud downloads need free-space, cancellation and partial-file cleanup before enabling them.
  Until those are implemented, cloud playback uses bounded memory buffering only.

## Stages

1. Inspect repositories and fetch sources. Complete.
2. Create branch from fetched origin/main. Complete.
3. Preserve toolchain and dependencies; establish supported API contract. Complete.
4. Implement API and Android Keystore credentials. Implemented; local tests in progress.
5. Implement bounded torrent, Usenet and web-download libraries and manual synchronization. Implemented.
6. Extend the current player for cloud sources and resume. Implemented; live playback unverified.
7. Connect navigation and settings. Implemented, including the app lock.
8. Create original vector assets and update Android/shared branding while preserving attribution. Implemented; website and release infrastructure remain upstream.
9. Compile and run existing and new tests; record actual results.
10. Audit changes, document limitations and commit locally.

## Progress / handoff

- Repository inspection and branch isolation complete. Both source working trees were clean.
- Installed environment: JDK 23, Android SDK 35. Documented build needs JDK 21 and SDK 37.
- Initial wrapper check failed downloading Gradle 9.8.1 with a connection timeout. Build
  prerequisites and network access must be resolved before an APK can be verified.
- Live TorBox account and real phone/TV tests require credentials and connected devices.

- JDK 21.0.13, SDK 37 and Build Tools 36 are now available locally; version pins were preserved.
- A debug APK was generated during the second validation pass. A final rebuild is required to
  include subsequent provider routes and credential-lock changes.
- Original SVG logos, PNGs, density-specific launchers, adaptive/monochrome layers and the TV
  banner are generated. XML/SVG syntax and launcher dimensions passed local checks.
- First provider tests: 6 passed. Garbage-input tests: 8 passed in the second pass.
- Full suites have failures, including missing Windows symbolic-link privilege, stale branding
  assertions and Android storage fixtures. Do not call regression validation complete.
- User chose local tests only. Live TorBox authentication, actual streaming, audio/subtitle
  decoding, real-device focus and low-end TV CPU checks remain unverified.
- Cloud downloads, persistent cloud-library caching, in-player cloud quality switching, cloud
  casting and automatic cloud episode advance are not implemented. Choose an available file
  version before playback; use the TorBox dashboard for downloads until a safe queue is added.
- New TorBox text currently falls back to English outside the English catalog. Translate and
  review the 20 other catalogs before release.

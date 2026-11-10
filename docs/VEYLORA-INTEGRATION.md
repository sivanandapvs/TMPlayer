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

Current scope update, 2026-11-09: the user requested the Android APK only. Further TV and
desktop/EXE work is deferred. The platform analysis below records the original integration design.


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
4. Implement API and Android Keystore credentials. Implemented; local API and session validation only, real account verification pending.
5. Implement bounded torrent, Usenet and web-download libraries and manual synchronization. Implemented.
6. Extend the current player for cloud sources and resume. Implemented; live playback unverified.
7. Connect navigation and settings. Implemented, including the app lock.
8. Create original vector assets and update Android/shared branding while preserving attribution. Implemented; website and release infrastructure remain upstream.
9. Build the current Android debug APK and run focused local checks. Broad regression failures remain open.
10. Record present work and deferred features in VEYLORA-PENDING.md, preserve evidence and commit locally.

## Progress / handoff

- 2026-11-10 credential phase: root `local.properties` was missing and was created locally
  with the actual SDK path and placeholder Telegram values. Git ignores it. The user has since
  supplied locally valid-format values. A safe comparison confirmed both reached the generated
  BuildConfig without printing them. Login on a device remains unverified. The build rejects
  incomplete or malformed credential pairs.
- The requested unified Home remains open. `TorBoxFile.media()` previously invented zero Telegram
  IDs. It now returns `TorBoxMedia`, carrying a provider-specific
  `MediaIdentity.TorBox` and using the shared filename parser. Home, details, progress, and
  metadata caches still key records through Telegram IDs. Wire those paths to the new identity
  before placing TorBox cards in Home.
  Keep the existing TorBox Activity for account management until the shared route is complete.
- Phone and TV behavior for the proposed catalogue: phone touch and rows; TV D-pad focus and
  restoration. Handle empty and partially failed cloud pages, disconnected accounts, unavailable
  files, duplicate titles across providers, multiple qualities and episodes, and independent
  provider refresh. Desktop and web are outside this Android pass.
- Baseline test audit: `:core:jvmTest` reported 2,124 tests, four failures and 18 skipped. The
  four failures are in `CacheDirRenameTest` and `WatchCacheRulesTest`: Windows denies symbolic
  link creation without privilege, while the rename code intentionally keeps the old path.
  `:app:testDebugUnitTest` also reported failures in downloads and two browse focus tests before
  the run stopped producing progress. The `LocalDownloadsTest` XML shows repeated DataStore
  `Unable to rename ...preferences_pb.tmp` errors on Windows, then the missing record assertion.
  Focused `TorBoxMediaIdentityTest` passes, as does `:app:compileDebugKotlin`.
- The final APK must wait for full Home, details, history, cache and TV integration and passing
  test suites. The APK from the prior credential-only pass predates this source change.

- Implementation source checkpoint: `ba3125a8`. No package ID migration, merge or publication.
- Android debug APK built successfully, signature verified. Final focused validation passed
  23 core tests and 7 Android tests. Artifact: `build/veylora/Veylora-debug.apk`.
- JDK 21.0.13, Gradle 9.8.1, SDK 37 and Build Tools 36 are available. Version pins are unchanged.
- Android TorBox account, encrypted credentials/cache, bounded libraries and the existing player
  handoff are implemented. Requests are cancellable and have a 45-second deadline; external
  JSON is bounded by size, nesting depth and entry count.
- Original SVG/PNG assets, Android launcher densities, adaptive/monochrome icons and splash
  resources are present. Original legal notices and upstream contributions remain attributed.
- The user chose local tests only and deferred TV and desktop/EXE work. Do not infer actual
  account authentication, streaming or phone compatibility from unit tests.
- Previous broad test runs had unresolved failures. Passing focused tests does not close those
  failures or establish complete preservation of every original feature.
- Current build evidence and exact validation counts: [VEYLORA-REPORT.md](VEYLORA-REPORT.md).
- Remaining features, release blockers and deferred platforms: [VEYLORA-PENDING.md](VEYLORA-PENDING.md).
- Resume from the pending checklist after reviewing the Android APK. Do not merge or publish
  until the user authorizes it. No further platform work is required in the present pass.

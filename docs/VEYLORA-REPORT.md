# Veylora Android build report

Date: 2026-11-09. Branch: `feature/torbox-integration-rebrand`.

## Scope

The user requested an Android APK checkpoint and deferred TV and desktop/EXE work.
The integration is not complete or release-ready. All outstanding work is recorded in
[VEYLORA-PENDING.md](VEYLORA-PENDING.md).

TMPlayer was fetched before branching from `origin/main` at `b4e93bf9`.
TorBox-Cinema `origin/main-2` at `746fa4b` was inspected as a behavioral reference.
No TorBox-Cinema source, native binaries or artwork was copied because its repository
does not provide a project-wide license. The adapter uses the documented TorBox API.

## Implemented checkpoint

One Android application retains the existing Telegram modules and player. TorBox adds
account validation, encrypted credentials, account information, local disconnect,
three cloud-library providers, bounded manual paging/refresh, encrypted first-page
snapshots, recent sorting, video/episode filtering and loaded-file search.

Cloud file versions hand off to the existing Media3/NextLib player. Audio and embedded
subtitle controls are reused. Local cloud resume and retry with a fresh stream URL are
implemented. No second network client or playback engine was imported.

Veylora branding includes an original V/play mark, wordmark variants, SVG/PNG assets,
launcher densities, adaptive and monochrome icons, splash resources and violet styling.
The application ID and existing storage identity remain unchanged. Original legal
notices and upstream contributions retain attribution.

## APK and final focused validation

APK source: `ba3125a8aadb13e65b59cd465a980d2134218a7e`. Subsequent handoff changes are documentation only.

- APK: [Veylora-debug.apk](../build/veylora/Veylora-debug.apk), 94,945,103 bytes, about 90.5 MiB.
- Standard output: `app/build/outputs/apk/debug/app-debug.apk` contains the same APK.
- SHA-256: `F1DEBC7926A019B78A6888572F124C0E2DD1925744F7B783B35720E127BE1A6B`.
- Debug signature: `apksigner verify --verbose` succeeded, v1 and v2 verified.
- APK metadata: label Veylora, package `com.tmplayer`, version 1.28.1, code 1280199,
  minimum API 23, target API 35, compile API 37.
- Native ABIs: arm64-v8a, armeabi-v7a, x86 and x86_64.

| Final-source check | Actual result |
| --- | --- |
| Gradle dependency/configuration resolution and Android debug compilation | Passed through command-line Gradle; Android Studio GUI sync was not run. |
| Debug APK plus focused core and Android test invocation | BUILD SUCCESSFUL, 7 minutes 26 seconds. |
| TorBox API, cache and 20,000-file scale tests | 13 passed, no failures or skips. |
| HTTP cancellation and connection reuse tests | 2 passed, using a local HTTP server. |
| Garbage-input corpus, final-source rerun | 8 passed. The 23-test combined core invocation completed successfully in 18 seconds. |
| Android account-session tests | 3 passed: rejected key replacement, failed refresh/local disconnect and cache before network. Uses fake transport and test-only vault. |
| Android playback intent/recovery tests | 4 passed. Verifies handoff metadata, credential failure and recovery actions, not successful media decoding. |

Final focused total: 30 tests passed, zero failed, zero skipped. The full regression suite
was not repeated on this final source; its outstanding failures below remain open.

Logs, XML results and HTML reports are preserved under
`build/reports/veylora/ba3125a8/`. Signature and APK metadata outputs are under
`build/veylora/`. These artifacts are local and ignored by Git.

## Earlier regression evidence

These earlier runs are evidence of open failures, not proof that the final source passes
the full regression suite. They were not all repeated after the final network/session changes.

| Run | Result |
| --- | --- |
| Shared core, checkpoint f4d4f416 | 2,120 tests: 2,098 passed, 4 failed, 18 skipped. |
| Shared UI, checkpoint f4d4f416 | 212 tests passed. |
| Targeted Android, checkpoint e3571b4e | 14 tests passed, including revised branding assertions and earlier focus checks. |
| Earlier broad Android run | 412 tests: 398 passed, 10 failed, 4 skipped. Two branding failures were fixed and passed targeted reruns. Eight other failures remain untriaged. |
| Garbage-input corpus, earlier run | 8 tests passed, including TorBox parser/cache coverage. |

The four core failures involve `WatchCacheRulesTest` and `CacheDirRenameTest`.
The former explicitly reports missing Windows symbolic-link privilege; the latter three
fail link/rename expectations on this host. No elevated privilege was requested.

The eight remaining broad Android failures are:

- `CrashExitsTest`: sending marks an exit asked without declining it.
- `DownloadFinisherTest`: a finished file moves into Downloads.
- `DownloadRelocationTreeTest`: a full picked folder stops the move.
- `DownloadTreeTest`: a full folder leaves no partial copy.
- `LegacyDownloadsTest`: whole files move, part files and missing ones stay.
- `LegacyDownloadsTest`: a saved ID from an earlier session is looked up again.
- `LocalDownloadsTest`: only present downloads are offered to play.
- `AddChatsTvKeysTest`: a held Down speeds up. TV investigation is deferred.

These failures have not been compared with a clean base, so they must not be described as
confirmed pre-existing failures. Their original broad XML remains under
`app/build/test-results/testDebugUnitTest` in the main checkout.

## Reproduce locally

Use JDK 21, Android SDK 37 and Build Tools 36. Existing Gradle, AGP and Kotlin pins remain
unchanged. See [dependency report](VEYLORA-DEPENDENCIES.md) and [BUILDING.md](BUILDING.md).

```powershell
$env:JAVA_HOME='C:/Users/wisewoods399/.cache/veylora-tools/jdk-21.0.13+9'
$env:ANDROID_HOME='C:/Users/wisewoods399/AppData/Local/Android/Sdk'
.\gradlew.bat :app:assembleDebug --max-workers=2 --no-parallel --console=plain
.\gradlew.bat :core:jvmTest --tests 'com.tmplayer.torbox.*' --tests 'com.tmplayer.online.UrlConnectionCancellationTest' --tests 'com.tmplayer.data.GarbageInputTest' --max-workers=2 --no-parallel --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests 'com.tmplayer.torbox.TorBoxSessionTest' --tests 'com.tmplayer.torbox.TorBoxPlaybackIntentTest' --max-workers=2 --no-parallel --console=plain
```

Telegram app credentials were not configured for this APK. Compilation supports that state;
Telegram sign-in requires adding your own credentials to ignored local configuration and
rebuilding. TorBox is accessible before Telegram sign-in and accepts a key inside the app.

No live TorBox account, phone, streaming session, track decoding, release/R8 build or desktop
installer was tested. No TV device validation is claimed.

## Review and handoff

- [Original logo assets](../art/veylora/)
- [Changed and new files](VEYLORA-FILES.md)
- [Upstream identity audit](VEYLORA-UPSTREAM-AUDIT.md)
- [Provenance and licenses](VEYLORA-PROVENANCE.md)
- [Pending work for later review](VEYLORA-PENDING.md)

Changes are committed locally. No merge, push, tag, release or deployment was performed.
If the user later chooses to back up this branch, run the following command themselves:

```powershell
git push -u origin feature/torbox-integration-rebrand
```

This publishes the feature branch and sets its upstream. Check the remote is the intended
fork and the branch name is correct. It does not merge into main or publish a release.

# Veylora pending work

Updated: 2026-11-09. Branch: `feature/torbox-integration-rebrand`.

The current deliverable is an Android debug APK. The user deferred TV and desktop/EXE work
and chose local tests instead of live account or device testing. This is a development
checkpoint, not a completed integration or release candidate.

## Implemented, awaiting live Android verification

- TorBox account validation, replacement, local disconnect and account information.
- Android Keystore credentials and encrypted first-page library snapshots.
- Torrent, Usenet and web-download libraries, manual refresh, paging and recent sorting.
- Video/episode filename parsing, loaded-file search and file-version selection.
- Existing Android player handoff, local resume and link renewal on retry.
- Existing audio and embedded subtitle controls reused for cloud playback.
- Veylora Android name, violet theme, original logo, density-specific launcher assets,
  adaptive and monochrome icons, splash screen and settings entry.

## Pending Android features and checks

| Work | Status and next step |
| --- | --- |
| Live TorBox authentication and library responses | Not tested with a real account. Enter a key on the device; never put it in chat or Git. |
| Real streaming, seek, completion and recovery | Not tested on a phone. Verify short files, expired links and network interruptions. |
| Audio/subtitle decoding and format coverage | Controls are reused; actual cloud tracks and codecs remain unverified. |
| Keystore round trip, upgrade and process death | Verify on API 23 and a current Android phone. Local tests do not prove device crypto behavior. |
| Cloud video downloads and offline video cache | Not implemented. Add shared queue support, space checks, cancellation and partial-file cleanup before enabling. |
| In-player cloud quality switching | Not implemented. Currently choose an available file version in the library before playback. |
| Automatic next episode and cloud casting | Not implemented. Preserve current Telegram behavior while designing cloud source identity. |
| Unified Continue Watching and cross-device resume | Cloud resume is local to this installation and separate from Telegram history. |
| External subtitle matching and discovery/indexers | No complete TorBox-Cinema feature parity. No unsupported API endpoints have been invented. |
| Larger libraries and metadata | At most 20,000 loaded files; search covers loaded pages. Only the first page per provider is cached. |
| Translations | New TorBox text uses English fallback. Translate and review the other 20 catalogs before release. |
| Regression failures | Earlier broad runs are not green. See the build report for exact failures; compare against a clean base before assigning cause. |
| Telegram sign-in in this APK | No Telegram app credentials were configured. Add your own credentials locally and rebuild to test Telegram sign-in. |
| Release validation | Release/R8, extended FFmpeg source build, installation/upgrade and physical-phone checks remain pending. |

## Explicitly deferred platforms

- Android TV: no separate TV deliverable or further TV validation in this pass. Existing
  manifest/navigation support remains; remote focus, device playback and idle CPU need later checks.
- Desktop/EXE: no installer built. Shared branding/assets were prepared earlier, but desktop
  cloud navigation, playback and Windows/Linux/macOS validation remain pending.
- Web: cloud credential/CORS design, UI and playback integration remain pending.

## Identity and publication

- The package ID remains `com.tmplayer`. Migration needs approval after build verification.
  Installing over another build with this ID may fail if signing keys differ. Avoid uninstalling
  an existing installation without backing up its data.
- Website, distribution metadata and original publishing workflows still contain upstream
  identity. They need a separate migration; no Veylora website/operator has been invented.
- Upstream automatic updates and donation UI are disabled for this development build.
- Privacy and corresponding-source links must be reviewed and reachable before distribution.
- Original copyright, GPL/OpenSSL notices and third-party attribution are retained.
- Three follow-up fixes have been pushed to `feature/torbox-integration-rebrand` (credential fallback/validation and CI branch triggers). No merge into main, tag, release or deployment was performed.

## Local housekeeping

The detached verification worktree was unregistered, but Windows left files in
`C:/Users/wisewoods399/.cache/veylora-verify-tree` after reporting a filename-too-long error.
Automatic approval review rejected the subsequent directory removal with "blocked by policy".
That residual directory remains for later manual cleanup. The APK, reports and source branch
are preserved independently in the main checkout; no other directory needs removal.

See [build and test report](VEYLORA-REPORT.md), [dependency decisions](VEYLORA-DEPENDENCIES.md),
[integration map](VEYLORA-INTEGRATION.md) and [changed files](VEYLORA-FILES.md).

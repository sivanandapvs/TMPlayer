# Veylora

Veylora is an Android media player built on TMPlayer. It preserves playback of videos already
in your own Telegram chats and adds an optional TorBox cloud-library destination.

Development is isolated on `feature/torbox-integration-rebrand`. This is an integration build,
not a published release. The application ID remains `com.tmplayer`.

## Android

- Existing phone and Android TV navigation, Telegram library and player remain available.
- Open **TorBox cloud library** before Telegram sign-in, or from Settings.
- Enter your own TorBox API key. Veylora validates it before saving it with Android Keystore.
- A bounded encrypted first-page cache appears while refresh checks the cloud library.
  Load subsequent pages explicitly; no background polling runs.
- Browse torrent, Usenet or web-download files, videos or episodes; search the files already loaded on the device.
- Choose a file version to play. The existing player supplies audio and embedded subtitle
  selection, seeking, resume and retry with a newly requested cloud link.
- No content sources or indexers are bundled. Use only files you are entitled to access.

Cloud downloads, cloud casting, automatic next-episode playback, external cloud subtitle
matching, desktop/web cloud screens and real-device validation are not complete. The integration
report records current validation and release blockers. Existing Telegram functionality remains
in its original modules. The website's upstream install instructions are not Veylora releases.

## Build and validate

JDK 21, Android SDK 37 and Build Tools 36 are required. See [BUILDING](docs/BUILDING.md).
Telegram credentials are optional for compilation; never commit API keys.

```powershell
.\gradlew.bat :core:jvmTest :ui:jvmTest :app:testDebugUnitTest :app:assembleDebug --max-workers=2
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Design and development

- [Integration map, stages and handoff](docs/VEYLORA-INTEGRATION.md)
- [Upstream provenance and identity decisions](docs/VEYLORA-PROVENANCE.md)
- [Original SVG and PNG logo assets](art/veylora/)
- Reproduce assets: `python scripts/build-veylora-assets.py` with Pillow and fonttools.

Veylora is a GPL-3.0-or-later derivative of TMPlayer. Original authors retain their copyrights.
See [LICENSE](LICENSE), [third-party notices](THIRD_PARTY_NOTICES.md) and the Git history.
It is an unofficial Telegram client, independent of Telegram and TorBox. Upstream Telegram
terms and legal context: https://tmplayer.org/legal/.

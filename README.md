# Veylora

Veylora is an Android media player built on TMPlayer. It preserves playback of videos already
in your own Telegram chats and adds an optional TorBox cloud-library destination.

Development is isolated on `feature/torbox-integration-rebrand`. This is an integration build,
not a published release. The application ID is `com.veylora`, so this build installs separately from the original TMPlayer app.

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
Telegram credentials are optional for compilation. To enable Telegram sign in locally, edit the
ignored repository-root `local.properties`: set `TG_API_ID` and `TG_API_HASH` to your own values
from [my.telegram.org](https://my.telegram.org) under API development tools, then rebuild. Keep
`sdk.dir` and any other local settings. A build with `TG_API_ID=0` and an empty hash can compile,
but Telegram sign in will report missing credentials. Never commit API keys.

```powershell
.\gradlew.bat :core:jvmTest :ui:jvmTest :app:testDebugUnitTest :app:assembleDebug --max-workers=2
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Design and development

- [Android build and test report](docs/VEYLORA-REPORT.md)
- [Pending work for later review](docs/VEYLORA-PENDING.md)
- [Integration map, stages and handoff](docs/VEYLORA-INTEGRATION.md)
- [Upstream provenance and identity decisions](docs/VEYLORA-PROVENANCE.md)
- [Original SVG and PNG logo assets](art/veylora/)
- Reproduce assets: `python scripts/build-veylora-assets.py` with Pillow and fonttools.

Veylora is a GPL-3.0-or-later derivative of TMPlayer. Original authors retain their copyrights.
See [LICENSE](LICENSE), [third-party notices](THIRD_PARTY_NOTICES.md) and the Git history.

## Credits and acknowledgements

**Veylora is a derivative of [TMPlayer](https://github.com/dracu-lah/TMPlayer), the original open-source Telegram media player.**

The original TMPlayer project, its architecture, source code, Telegram integration, playback foundations, and contributions by its author and community remain credited to the original TMPlayer project and its contributors.

Veylora adds independently developed changes, including its branding and visual identity, TorBox integration, interface modifications, and additional features. These changes do not transfer ownership of the original TMPlayer work.

Thank you to the original TMPlayer author and contributors for making this derivative project possible.

- Original project: https://github.com/dracu-lah/TMPlayer
- Original project website: https://tmplayer.org/
- Original licence: GNU GPL-3.0-or-later

All original copyright notices, licence terms, and third-party attributions remain in force. See [LICENSE](LICENSE), [LICENSE-OPENSSL-EXCEPTION.md](LICENSE-OPENSSL-EXCEPTION.md), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

[GNU GPL-3.0-or-later](LICENSE) © 2026 dracu-lah, with an [additional permission for OpenSSL](LICENSE-OPENSSL-EXCEPTION.md).
The original player artwork retained in `art/player.svg` came from [SVG Repo](https://www.svgrepo.com).
Veylora's production V/play symbol is newly drawn; see `art/veylora/` and the provenance document.

It is an unofficial Telegram client, independent of Telegram and TorBox. Upstream Telegram
terms and legal context: https://tmplayer.org/legal/.

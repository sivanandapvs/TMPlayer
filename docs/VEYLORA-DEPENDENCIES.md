# Dependency and toolchain report

No runtime dependencies or version pins were added or replaced for TorBox.

| Concern | Decision |
| --- | --- |
| Gradle | Keep 9.8.1 and its checked-in distribution SHA-256 |
| Android Gradle Plugin | Keep 9.4.1 |
| Kotlin | Keep 2.4.21 |
| SDK | Keep compile 37, target 35, minimum 23 |
| Media3 / NextLib | Keep the existing versions and renderer setup |
| TDLib | Keep existing integration and ABIs |
| TorBox HTTP | Existing core HttpTransport and UrlConnectionTransport |
| TorBox JSON | Existing com.tmplayer.json wrapper over kotlinx serialization |
| Credential encryption | Android Keystore AES-256/GCM, available from API 23 |
| Credential file | Platform AtomicFile in noBackupFilesDir |
| TorBox UI | Existing Compose Material 3, lifecycle and shared theme |
| TorBox-Cinema MPV and ExoPlayer | Do not import; reuse PlayerActivity |
| TorBox-Cinema OkHttp and security-crypto | Do not import |
| Asset generation | Pillow 10.4.0 and fonttools 4.61.1 on the development machine only |

The repository's documented JDK 21 and Android SDK 37 were prepared locally during validation.
The system default Java remains unchanged. Gradle's first wrapper download timed out; a direct
download of the same distribution succeeded and matched the checked-in SHA-256.

Debug builds use the existing Maven NextLib fallback. They do not include the additional five
video decoders from the project's separately built FFmpeg extension. Release compilation and
native decoder source builds were not requested or published.

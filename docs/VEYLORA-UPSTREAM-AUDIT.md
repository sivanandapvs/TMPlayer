# Upstream reference audit

Every `dracu-lah` occurrence found by `rg --hidden` in the working tree, excluding Git metadata,
this generated report and ignored build outputs. Latest refresh: 2026-11-09.

Runtime source and issue links point to the current fork. Original copyrights and licenses remain.
Upstream donation UI and automatic updates are disabled. Historical release records, source offers,
package IDs, website deployment configuration and publishing automation retain original ownership.
Website and distribution migration are pending. Do not run original release workflows for Veylora.

Occurrences: 214.

| Location | Disposition |
| --- | --- |
| `.github/FUNDING.yml:1` | Original publishing automation; do not run for Veylora until migrated. |
| `.github/ISSUE_TEMPLATE/config.yml:4` | Original publishing automation; do not run for Veylora until migrated. |
| `.github/ISSUE_TEMPLATE/config.yml:10` | Original publishing automation; do not run for Veylora until migrated. |
| `.github/actions/desktop-linux/action.yml:88` | Original publishing automation; do not run for Veylora until migrated. |
| `.github/workflows/release.yml:546` | Original publishing automation; do not run for Veylora until migrated. |
| `.github/workflows/release.yml:776` | Original publishing automation; do not run for Veylora until migrated. |
| `README.md:49` | Keep original copyright, attribution and source provenance. |
| `SECURITY.md:19` | Retained upstream reference; review before a Veylora release. |
| `app/src/test/java/com/tmplayer/ui/update/ReleasePageTest.kt:13` | Historical test data or assertion; review if the associated runtime URL changes. |
| `app/src/test/java/com/tmplayer/ui/update/ReleasePageTest.kt:25` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/commonMain/kotlin/com/tmplayer/data/UpdateFeed.kt:63` | Upstream update protocol, disabled in the Android build. |
| `core/src/commonMain/kotlin/com/tmplayer/data/Updates.kt:73` | Upstream update protocol, disabled in the Android build. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:26` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:51` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:70` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:82` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:229` | Historical test data or assertion; review if the associated runtime URL changes. |
| `core/src/jvmTest/kotlin/com/tmplayer/data/UpdateFeedTest.kt:233` | Historical test data or assertion; review if the associated runtime URL changes. |
| `desktop/build.gradle.kts:499` | Upstream attribution/configuration retained; inspect before publishing. |
| `desktop/packaging/aur/.SRCINFO:27` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/aur/PKGBUILD:1` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/aur/PKGBUILD:25` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:30` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:51` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:52` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:79` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:83` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/io.github.dracu_lah.TMPlayer.metainfo.xml:90` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/packaging/linux/package.sh:119` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `desktop/src/test/kotlin/com/tmplayer/desktop/os/OpenExternalTest.kt:12` | Historical test data or assertion; review if the associated runtime URL changes. |
| `desktop/src/test/kotlin/com/tmplayer/desktop/ui/BrowseRenderTest.kt:466` | Historical test data or assertion; review if the associated runtime URL changes. |
| `desktop/src/test/kotlin/com/tmplayer/desktop/ui/LanguageShotsTest.kt:101` | Historical test data or assertion; review if the associated runtime URL changes. |
| `desktop/src/test/kotlin/com/tmplayer/desktop/ui/SupportRenderTest.kt:65` | Historical test data or assertion; review if the associated runtime URL changes. |
| `docs/RELEASING.md:185` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/RELEASING.md:215` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/RELEASING.md:227` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/VEYLORA-PROVENANCE.md:5` | Keep original copyright, attribution and source provenance. |
| `docs/VEYLORA-PROVENANCE.md:6` | Keep original copyright, attribution and source provenance. |
| `docs/agents/branches.md:8` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/agents/branches.md:15` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/amazon.md:51` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:41` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:66` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:114` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:139` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:140` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:144` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:154` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/telegram-apps.md:157` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget.md:43` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.installer.yaml:24` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.locale.en-US.yaml:9` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.locale.en-US.yaml:10` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.locale.en-US.yaml:11` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.locale.en-US.yaml:15` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `docs/distribution/winget/TMPlayer.TMPlayer.locale.en-US.yaml:41` | Historical documentation or retained package identity; do not claim a Veylora release. |
| `scripts/aur-bump.py:24` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/build-mediamp-source.sh:319` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/build-mediamp-source.sh:383` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/build-nextlib-source.sh:138` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/devicepass/vm.sh:65` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/download-counts.sh:20` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/download-counts.sh:24` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/post-release-telegram.py:27` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/release-data.py:49` | Original publishing automation; do not run for Veylora until migrated. |
| `scripts/write-latest-json.py:26` | Original publishing automation; do not run for Veylora until migrated. |
| `site/i18n/ar.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ar.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ar.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/bn.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/bn.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/bn.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/de.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/de.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/de.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/en.json:138` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/en.json:262` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/en.json:1048` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/en.json:1794` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/en.json:1819` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/es-419.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/es-419.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/es-419.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fa.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fa.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fa.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fr.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fr.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/fr.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/hi.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/hi.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/hi.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/id.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/id.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/id.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/it.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/it.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/it.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ja.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ja.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ja.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ko.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ko.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ko.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ml.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ml.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ml.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pl.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pl.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pl.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pt-BR.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pt-BR.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/pt-BR.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ru.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ru.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/ru.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/tr.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/tr.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/tr.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/uk.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/uk.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/uk.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/vi.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/vi.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/vi.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-CN.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-CN.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-CN.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-TW.json:129` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-TW.json:250` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/i18n/zh-TW.json:1030` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/public/.well-known/security.txt:3` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/public/install.ps1:344` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/public/latest.json:6` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/public/llms.txt:16` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/public/llms.txt:57` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/Footer.astro:26` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/Footer.astro:68` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/Footer.astro:83` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/GiveBand.astro:30` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/Header.astro:42` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/Header.astro:47` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/StarLCard.astro:24` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/download/AbiRow.astro:27` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/download/NextSteps.astro:75` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/faq/ld.ts:212` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/components/guides/ld.ts:45` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/alternatives.json:6` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/alternatives.json:7` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:5` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:12` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:19` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:26` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:33` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:40` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:47` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:54` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:61` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:68` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:75` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:82` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:89` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/data/releases.json:96` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/i18n/ld.ts:9` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/layouts/Base.astro:89` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/lib/app-ld.ts:28` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/lib/releases.ts:10` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/lib/stats.ts:33` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/lib/support.ts:5` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/download/index.astro:39` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/download/index.astro:213` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/download/index.astro:273` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/download/index.astro:335` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/faq/index.astro:135` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/features/index.astro:234` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/guides/telegram-videos-on-fire-tv/index.astro:153` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:85` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:89` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:94` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:153` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:154` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:157` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/index.astro:237` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:100` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:168` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:210` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:231` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:237` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/install/index.astro:320` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/translate/index.astro:60` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/translate/index.astro:73` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/translate/index.astro:76` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/[...lang]/translate/index.astro:92` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/changelog/[version]/index.astro:41` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/legal/index.astro:29` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/legal/index.astro:37` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/legal/index.astro:46` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/privacy/index.astro:20` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:36` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:60` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:61` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:70` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:90` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:119` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/security/index.astro:123` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/signing/index.astro:31` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/support/index.astro:48` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/pages/support/index.astro:52` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/scripts/app.js:42` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `site/src/scripts/app.js:1001` | Existing upstream website/hosting reference; Veylora web deployment is pending. |
| `ui/src/shared/kotlin/com/tmplayer/ui/about/About.kt:28` | Upstream sponsorship attribution, hidden by SUPPORT_LINKS=false. |
| `webapp/libav/sources.sh:30` | Keep upstream attribution or source provenance. |
| `webapp/notices.config.ts:39` | Keep upstream attribution or source provenance. |
| `workers/download-mirror/src/index.ts:39` | Existing upstream website/hosting reference; Veylora web deployment is pending. |

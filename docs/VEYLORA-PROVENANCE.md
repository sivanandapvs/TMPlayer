# Veylora provenance and identity

Veylora is a derivative of TMPlayer, licensed under GPL-3.0-or-later. TMPlayer's authors and
contributors retain their copyrights. See LICENSE, THIRD_PARTY_NOTICES.md and Git history.
The upstream source is https://github.com/dracu-lah/TMPlayer.
Original notice: GNU GPL-3.0-or-later, © 2026 dracu-lah, with the additional permission for
OpenSSL in LICENSE-OPENSSL-EXCEPTION.md. The original SVG Repo artwork remains attributed.

TorBox-Cinema main-2 at 746fa4b was inspected to identify cloud-library behavior and build
differences. No TorBox-Cinema application source, bundled native library or artwork is copied.
There is no project-wide license in that repository's tracked root. Any future source reuse
requires a compatible license or permission from the relevant rights holders.

The TorBox implementation uses the official contract at https://api.torbox.app/openapi.json
and https://api-docs.torbox.app/. Only the documented account, torrent list and download-link
endpoints are called. TorBox is a third-party service and its name remains its own.

The Veylora V/play symbol is newly drawn vector geometry. Sources and exports are in
art/veylora. scripts/build-veylora-assets.py reproduces Android and desktop assets using Pillow
and fonttools. The wordmark outlines are rendered from the locally installed Segoe UI Bold;
no font file is redistributed. The Android API 23 vectors use solid facets; PNG exports have
gradients. The adaptive mark stays within the launcher safe area.

The Android application ID, Kotlin packages, existing storage names and signing setup remain
unchanged. A package migration is a separate decision after a successful build and approval.
Upstream historical release data, attribution, package-manager identities and existing protocol
identifiers must not be rewritten as if Veylora originally published them.

The existing tmplayer.org legal document remains an upstream reference. This branch provides
its own observed data-flow document in VEYLORA-PRIVACY.md, linked from About. No Veylora
domain, privacy operator, donation account or release infrastructure has been supplied. This
branch must not be published until those decisions are reviewed. Android donation prompts and
upstream update checks are disabled in this development build.

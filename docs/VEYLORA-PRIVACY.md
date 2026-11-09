# Veylora development build: data and privacy

This document describes the integration branch, not a published service or a new operator of
tmplayer.org. Veylora is derived from TMPlayer; the original copyright and license notices remain.
Questions about this fork belong in https://github.com/sivanandapvs/TMPlayer/issues.
No Veylora support email or donation account is configured.

## TorBox

Connecting sends the API key to TorBox over HTTPS to validate the account. The key is encrypted
with AES-256/GCM using Android Keystore and saved in the app's private no-backup directory.
It is not included in the app's settings export. There is no plaintext-storage fallback.

Account information and cloud-library file names are requested directly from TorBox. The first library page for each provider is cached locally with Keystore-backed encryption,
bounded to 8 MB per provider. Other loaded pages stay in memory in this build. Refresh and paging are requested by the viewer; there is no
automatic cloud-library polling. Provider availability depends on the TorBox account and plan.

Playing requests a temporary download URL from TorBox, then sends playback requests to that URL's
HTTPS host. TorBox requires the API key in the download-link request's query as well as the
authorization header. Veylora does not intentionally log that request or store signed playback
URLs in Android intents or resume preferences. TorBox and its delivery hosts receive network
information needed to serve those requests and apply their own policies.

Cloud resume preferences contain a hashed account/provider/file identifier and the playback
position. They are bounded to 500 entries. Disconnect removes the encrypted API key, attempts to remove encrypted library snapshots,
and clears the displayed library. It does not delete cloud files on TorBox. Resume positions remain local;
clearing Android app data removes them.

## Existing features

Telegram sign-in and video access continue through TDLib. Existing optional metadata, online
subtitle, casting and crash-reporting features retain their settings and third-party data flows.
Using optional online metadata or subtitle search can send a video's name or identifiers to the
selected provider. Log export remains a user action. Review logs before sharing them.

Android backups are disabled for this application, and the TorBox credential file additionally
uses noBackupFilesDir. Reinstallation or clearing app data may require connecting again.

This development build disables upstream update checks and donation prompts. It has not been
verified against a live TorBox account or on a real phone/TV. Deployment-specific privacy and
contact details must be reviewed before publishing a Veylora release or website.

# Split ZIP playback

TMPlayer recognises raw byte-split archives named `Movie.zip.001`, `Movie.zip.002`, and so on. Volumes are grouped only within the same Telegram chat and exact archive base name, ignoring case. They must start at `001`, have no gaps, and have no duplicate numbers. Missing and duplicate numbers are shown before playback.

For an inner video stored without compression (ZIP method 0), TMPlayer reads the ZIP directory and the video's local header, then streams only the video's byte range. It can seek across volume boundaries. Each volume can come from an existing local download or from TDLib range streaming. The archive is not extracted into memory or a temporary video file.

Deflate entries (method 8), encrypted entries, and other compression methods cannot play yet. Deflate bytes cannot be used as seekable video bytes. Traditional ZIP archives that span ZIP disks rather than raw byte-split files are unsupported.

Built-in playback and casting use the selected inner video stream. External-player handoff is unavailable for split ZIPs because there is no complete extracted video file to share.

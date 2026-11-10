package com.tmplayer.torbox

import com.tmplayer.data.MediaIdentity
import com.tmplayer.data.MediaItem
import com.tmplayer.data.MediaName
import com.tmplayer.data.MediaText
import com.tmplayer.data.ParsedName

/** Cloud file information without a fabricated Telegram message or file reference. */
data class TorBoxMedia(val file: TorBoxFile) {
    val identity: MediaIdentity.TorBox = MediaIdentity.TorBox(file.source, file.torrentId, file.fileId)
    val fileName: String = file.name.substringAfterLast('/').substringAfterLast('\\')
    val parsedName: ParsedName = MediaName.parse(fileName)
    val qualityTags: List<String> = MediaText.qualityTags(fileName)
    val title: String get() = parsedName.title.ifBlank { fileName }
    val sourceLabel: String get() = "TorBox"

    fun toMediaItem(): MediaItem {
        val name = file.name.substringAfterLast('/').substringAfterLast('\\')
        return MediaItem(
            chatId = 0L,
            messageId = 0,
            fileId = 0,
            title = title,
            sizeBytes = file.size,
            durationSec = 0,
            mimeType = "video/*",
            thumbnailFileId = 0,
            miniThumbnail = null,
            date = 0,
            fileName = name,
            caption = "",
            identity = identity,
        )
    }
}

/** Media from a TorBox file. */
fun TorBoxFile.media(): TorBoxMedia = TorBoxMedia(this)

/** Convert a TorBox file to a MediaItem with proper TorBox identity. Uses real identity instead of dummy Telegram fields. */
fun TorBoxFile.toMediaItem(): MediaItem {
    val name = this.name.substringAfterLast('/').substringAfterLast('\\')
    return MediaItem(
        chatId = 0L,
        messageId = 0,
        fileId = 0,
        title = name,
        sizeBytes = size,
        durationSec = 0,
        mimeType = "video/*",
        thumbnailFileId = 0,
        miniThumbnail = null,
        date = 0,
        fileName = name,
        caption = "",
        identity = MediaIdentity.TorBox(source, torrentId, fileId),
    )
}
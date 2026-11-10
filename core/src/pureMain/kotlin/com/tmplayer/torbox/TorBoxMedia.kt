package com.tmplayer.torbox

import com.tmplayer.data.MediaIdentity
import com.tmplayer.data.MediaName
import com.tmplayer.data.MediaText
import com.tmplayer.data.ParsedName

import com.tmplayer.data.MediaItem

/** Cloud file information without a fabricated Telegram message or file reference. */
data class TorBoxMedia(val file: TorBoxFile) {
    val identity: MediaIdentity.TorBox = MediaIdentity.TorBox(file.source, file.torrentId, file.fileId)
    val fileName: String = file.name.substringAfterLast('/').substringAfterLast('\\')
    val parsedName: ParsedName = MediaName.parse(fileName)
    val qualityTags: List<String> = MediaText.qualityTags(fileName)
    val title: String get() = parsedName.title.ifBlank { fileName }
    val sourceLabel: String get() = "TorBox"

    fun toMediaItem(): MediaItem = MediaItem(
        chatId = 0L,
        messageId = 0,
        fileId = 0,
        title = title,
        sizeBytes = file.size,
        durationSec = 0,
        mimeType = "video/*",
        thumbnailFileId = 0,
        miniThumbnail = null,
        date = 0, // Should probably parse date
        fileName = fileName,
        caption = "",
        identity = identity
    )
}

fun TorBoxFile.media(): TorBoxMedia = TorBoxMedia(this)
fun TorBoxFile.toMediaItem(): MediaItem = media().toMediaItem()

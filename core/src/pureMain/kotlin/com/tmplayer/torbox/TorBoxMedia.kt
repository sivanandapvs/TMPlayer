package com.tmplayer.torbox

import com.tmplayer.data.MediaItem

/** The existing filename, episode and quality rules apply to cloud files too. */
fun TorBoxFile.media(): MediaItem = MediaItem(
    chatId = 0,
    messageId = 0,
    fileId = 0,
    title = name.substringAfterLast('/').substringAfterLast('\\'),
    sizeBytes = size,
    durationSec = 0,
    mimeType = "",
    thumbnailFileId = 0,
    miniThumbnail = null,
    date = 0,
    fileName = name.substringAfterLast('/').substringAfterLast('\\'),
)

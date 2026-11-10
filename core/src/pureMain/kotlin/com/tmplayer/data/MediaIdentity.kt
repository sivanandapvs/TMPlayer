package com.tmplayer.data

import com.tmplayer.torbox.TorBoxSource

/** A provider's own file identity. Telegram's persisted chat and message keys stay unchanged. */
sealed interface MediaIdentity {
    /** The stable key for this media item across sessions and providers. */
    val key: String

    /** The provider this identity belongs to. */
    val provider: Provider

    enum class Provider { Telegram, TorBox }

    /** Telegram's persisted chat and message keys. */
    data class Telegram(
        val chatId: Long,
        val messageId: Long,
        val fileId: Int = 0
    ) : MediaIdentity {
        override val key: String get() = "$chatId:$messageId"
        override val provider: Provider = Provider.Telegram
    }

    /** TorBox cloud media identifier. */
    data class TorBox(
        val source: TorBoxSource,
        val transferId: Long,
        val fileId: Long
    ) : MediaIdentity {
        override val key: String get() = "torbox:${source.path}:$transferId:$fileId"
        override val provider: Provider = Provider.TorBox
    }

    companion object {
        /** Parse a key string back into a MediaIdentity. */
        fun parse(key: String): MediaIdentity? {
            // TorBox format: torbox:torrents:123:456 or torbox:usenet:123:456
            if (key.startsWith("torbox:")) {
                val parts = key.split(':')
                if (parts.size >= 4) {
                    val src = TorBoxSource.entries.firstOrNull { it.path == parts[1] } ?: TorBoxSource.Torrents
                    val tid = parts[2].toLongOrNull() ?: return null
                    val fid = parts[3].toLongOrNull() ?: return null
                    return TorBox(src, tid, fid)
                }
            }
            // Legacy format: telegram:123:456
            if (key.startsWith("telegram:")) {
                val parts = key.split(':')
                if (parts.size >= 3) {
                    val cid = parts[1].toLongOrNull() ?: return null
                    val mid = parts[2].toLongOrNull() ?: return null
                    return Telegram(cid, mid, 0)
                }
            }
            // Standard format: 123:456 for Telegram
            if (key.contains('_')) {
                val parts = key.split('_')
                if (parts.size == 2) {
                    val cid = parts[0].toLongOrNull() ?: return null
                    val mid = parts[1].toLongOrNull() ?: return null
                    return Telegram(cid, mid, 0)
                }
            }
            // Try to parse as standard Telegram key (just chatId:messageId)
            if (!key.startsWith("torbox:")) {
                val parts = key.split(':')
                if (parts.size == 2) {
                    val cid = parts[0].toLongOrNull() ?: return null
                    val mid = parts[1].toLongOrNull() ?: return null
                    return Telegram(cid, mid, 0)
                }
            }
            return null
        }
    }
}

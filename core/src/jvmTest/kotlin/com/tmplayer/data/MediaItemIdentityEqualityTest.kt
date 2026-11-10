package com.tmplayer.data

import com.tmplayer.torbox.TorBoxSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaItemIdentityEqualityTest {
    private fun torBox(
        transferId: Long = 42L,
        fileId: Long = 7L,
        title: String = "Example 2026",
    ) = MediaItem(
        chatId = 0L,
        messageId = 0L,
        fileId = 0,
        title = title,
        sizeBytes = 100L,
        durationSec = 120,
        mimeType = "video/*",
        thumbnailFileId = 0,
        miniThumbnail = null,
        date = 1_794_000_000,
        fileName = "Example.2026.1080p.mkv",
        identity = MediaIdentity.TorBox(TorBoxSource.Torrents, transferId, fileId),
    )

    @Test
    fun `same TorBox identity still notices changed metadata`() {
        val before = torBox()
        val after = before.copy(title = "Example (2026)")
        assertNotEquals(before, after)
    }

    @Test
    fun `different TorBox files never collide on fabricated Telegram fields`() {
        val first = torBox(transferId = 42L, fileId = 7L)
        val second = torBox(transferId = 43L, fileId = 8L)
        assertNotEquals(first, second)
    }

    @Test
    fun `equal identity and content have equal hash codes`() {
        val first = torBox()
        val second = torBox()
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `explicit Telegram identity remains compatible with legacy Telegram item`() {
        val legacy = MediaItem(
            chatId = 55L,
            messageId = 91L,
            fileId = 12,
            title = "Telegram video",
            sizeBytes = 1_000L,
            durationSec = 120,
            mimeType = "video/mp4",
            thumbnailFileId = 0,
            miniThumbnail = null,
            date = 100,
            fileName = "video.mp4",
        )
        val explicit = legacy.copy(identity = MediaIdentity.Telegram(55L, 91L, 12))
        assertEquals(legacy, explicit)
        assertEquals(legacy.hashCode(), explicit.hashCode())
    }
}

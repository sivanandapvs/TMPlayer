package com.tmplayer.ui.browse

import com.tmplayer.data.HomeRow
import com.tmplayer.data.MediaItem
import com.tmplayer.data.ShelfEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeFeaturedFallbackTest {

    private fun item() = MediaItem(
        chatId = -100L,
        messageId = 12L,
        fileId = 7,
        title = "Night Train 2026 1080p.mkv",
        sizeBytes = 1_500_000_000L,
        durationSec = 5_400,
        mimeType = "video/x-matroska",
        thumbnailFileId = 99,
        miniThumbnail = null,
        date = 1_800_000_000,
        fileName = "Night.Train.2026.1080p.mkv",
    )

    @Test
    fun `loaded local items remain featured without online metadata`() {
        val media = item()
        val recent = HomeRow.Recent(
            entries = listOf(ShelfEntry.File(media)),
            loaded = true,
        )

        val picks = featuredCandidatesForHome(recent)

        assertEquals(listOf(media.id), picks?.map { it.item.id })
    }

    @Test
    fun `recent shelf stays pending until its local items have loaded`() {
        val recent = HomeRow.Recent(
            entries = listOf(ShelfEntry.File(item())),
            loaded = false,
        )

        assertNull(featuredCandidatesForHome(recent))
    }
}

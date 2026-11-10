package com.tmplayer.torbox

import com.tmplayer.data.MediaIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TorBoxMediaIdentityTest {
    @Test fun identityIncludesProviderTransferAndFile() {
        val sameName = "Example.2026.1080p.mkv"
        val files = TorBoxSource.entries.map { TorBoxFile(42, 7, sameName, 100, "2026-11-01", true, it) }
        val keys = files.map { it.media().identity.key }
        assertEquals(3, keys.toSet().size)
        assertNotEquals(keys.first(), MediaIdentity.Telegram(42, 7, 7).key)
        assertNotEquals(keys.first(), files.first().copy(fileId = 8).media().identity.key)
        assertEquals("TorBox", files.first().media().sourceLabel)
    }

    @Test fun usesTheSharedFilenameParserForEpisodesAndQuality() {
        val media = TorBoxFile(42, 7, "shows/Example.S02E03.4K.mkv", 100, "2026-11-01", true).media()
        assertEquals(2, media.parsedName.season)
        assertEquals(3, media.parsedName.episode)
        assertEquals("Example.S02E03.4K.mkv", media.fileName)
        assertEquals(listOf("4K"), media.qualityTags)
    }
}

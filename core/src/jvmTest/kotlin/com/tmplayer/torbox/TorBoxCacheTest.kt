package com.tmplayer.torbox

import kotlin.test.*

class TorBoxCacheTest {
    private val source = TorBoxSource.Usenet
    private val page = TorBoxPage(listOf(
        TorBoxFile(7, 0, "Example.S01E01.1080p.mkv", 5000, "2026-11-01", true, source),
        TorBoxFile(7, 1, "Example.S01E02.1080p.mkv", 6000, "2026-11-01", true, source),
    ), 100)

    @Test fun aSnapshotPayloadRetainsFileIdentityAndContinuation() {
        val text = TorBoxApi.cachePage(page, "owner", source)
        assertEquals(page, TorBoxApi.cachedPage(text, "owner", source))
        assertFalse(text.contains("https://"))
        assertFalse(text.contains("api_key"))
    }

    @Test fun aCacheFromAnotherAccountOrProviderIsNeverShown() {
        val text = TorBoxApi.cachePage(page, "owner", source)
        assertNull(TorBoxApi.cachedPage(text, "another-owner", source))
        assertNull(TorBoxApi.cachedPage(text, "owner", TorBoxSource.Web))
        assertNull(TorBoxApi.cachedPage(text.replace("\"version\":1", "\"version\":99"), "owner", source))
    }

    @Test fun malformedContinuationAndTruncatedSnapshotsAreRejected() {
        val text = TorBoxApi.cachePage(page, "owner", source)
        assertNull(TorBoxApi.cachedPage(text.dropLast(5), "owner", source))
        assertNull(TorBoxApi.cachedPage(text.replace("\"next_offset\":100", "\"next_offset\":123"), "owner", source))
        assertNull(TorBoxApi.cachedPage(text.replace("\"data\":[", "\"data\":{\"bad\":["), "owner", source))
    }
}

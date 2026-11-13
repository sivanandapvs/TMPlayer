package com.tmplayer.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitZipTest {
    private fun file(chat: Long, id: Long, name: String) = ArchiveFile(chat, id, id.toInt(), name, 100, 0, ArchiveKind.Zip, true)

    @Test fun `parsing is ZIP specific and grouping stays within chat and exact base`() {
        assertNull(SplitZip.parse(file(1, 1, "Movie.mkv.001")))
        assertNull(SplitZip.parse(file(1, 1, "Movie.zip.01")))
        val first = SplitZip.parse(file(1, 1, " Movie.ZIP.001 "))!!
        assertEquals(1L to "movie.zip", first.key)
        val group = SplitZip.group(first, listOf(file(1, 10, "Movie.zip.010"), file(1, 2, "movie.zip.002"), file(2, 3, "movie.zip.003"), file(1, 4, "Movie 2.zip.003")))
        assertEquals(listOf(1, 2, 10), group.volumes.map { it.number })
        assertEquals((3..9).toList(), group.missing)
        assertFalse(group.complete)
    }

    @Test fun `duplicate and absent first are reported`() {
        val second = SplitZip.parse(file(1, 2, "a.zip.002"))!!
        val group = SplitZip.group(second, listOf(file(1, 3, "a.zip.002")))
        assertEquals(listOf(1), group.missing)
        assertEquals(listOf(2), group.duplicates)
        assertTrue(SplitZip.group(SplitZip.parse(file(1, 1, "a.zip.001"))!!, listOf(file(1, 2, "a.zip.002"))).complete)
    }
}

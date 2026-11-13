package com.tmplayer.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZipEntriesTest {
    private fun archive(method: Int = ZipEntry.STORED): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun add(name: String, bytes: ByteArray) {
                val entry = ZipEntry(name)
                entry.method = method
                if (method == ZipEntry.STORED) {
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = entry.size
                    entry.crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            add("cover.jpg", byteArrayOf(9, 9))
            add("Subs.srt", byteArrayOf(8))
            add("Film 🎬.mkv", byteArrayOf(1, 2, 3, 4, 5))
        }
        return out.toByteArray()
    }

    private fun resolve(bytes: ByteArray): ZipVideoEntry = ZipEntries.video(bytes.size.toLong(), ZipRange { at, length ->
        bytes.copyOfRange(at.toInt(), at.toInt() + length)
    })

    @Test fun `stored entry points only at video bytes across virtual volume seam`() {
        val bytes = archive()
        val seam = bytes.indexOf(1).coerceAtLeast(1) + 2
        val volumes = listOf(bytes.copyOfRange(0, seam), bytes.copyOfRange(seam, bytes.size))
        val entry = ZipEntries.video(bytes.size.toLong(), ZipRange { at, length ->
            val joined = volumes[0] + volumes[1]
            joined.copyOfRange(at.toInt(), at.toInt() + length)
        })
        assertEquals("Film 🎬.mkv", entry.name)
        assertEquals(0, entry.method)
        assertEquals(5, entry.size)
        assertEquals(listOf<Byte>(1, 2, 3, 4, 5), bytes.copyOfRange(entry.dataOffset.toInt(), entry.dataOffset.toInt() + 5).toList())
        assertEquals(listOf<Byte>(3, 4), bytes.copyOfRange(entry.dataOffset.toInt() + 2, entry.dataOffset.toInt() + 4).toList())
    }

    @Test fun `data descriptor reads sizes from central directory`() {
        assertEquals(8, resolve(archive(ZipEntry.DEFLATED)).method)
    }

    @Test fun `ZIP64 extra field supplies sizes and local offset`() {
        val original = archive()
        val signature = byteArrayOf(0x50, 0x4b, 0x01, 0x02)
        val first = original.indexOfSequence(signature)
        val second = original.indexOfSequence(signature, first + 4)
        val record = original.indexOfSequence(signature, second + 4)
        val nameLength = (original[record + 28].toInt() and 255) or ((original[record + 29].toInt() and 255) shl 8)
        val insertAt = record + 46 + nameLength
        val extra = ByteArray(28)
        extra[0] = 1; extra[2] = 24
        extra[4] = 5; extra[12] = 5
        val local = (original[record + 42].toInt() and 255) or ((original[record + 43].toInt() and 255) shl 8)
        extra[20] = local.toByte(); extra[21] = (local ushr 8).toByte()
        val patched = original.copyOfRange(0, insertAt) + extra + original.copyOfRange(insertAt, original.size)
        for (p in listOf(20, 24, 42)) for (i in 0..3) patched[record + p + i] = 0xff.toByte()
        patched[record + 30] = 28
        val eocd = patched.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x05, 0x06))
        val oldSize = (original[eocd - 28 + 12].toInt() and 255) or ((original[eocd - 28 + 13].toInt() and 255) shl 8)
        patched[eocd + 12] = (oldSize + 28).toByte()
        patched[eocd + 13] = ((oldSize + 28) ushr 8).toByte()
        assertEquals(5, resolve(patched).size)
    }

    @Test fun `ZIP64 end record supplies directory position`() {
        val original = archive()
        val end = original.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x05, 0x06))
        fun number(source: ByteArray, at: Int, length: Int): Long =
            (0 until length).fold(0L) { value, i -> value or ((source[at + i].toLong() and 255L) shl (8 * i)) }
        fun put(target: ByteArray, at: Int, value: Long, length: Int) {
            for (i in 0 until length) target[at + i] = (value ushr (8 * i)).toByte()
        }
        val record = ByteArray(56)
        put(record, 0, 0x06064b50L, 4)
        put(record, 4, 44, 8)
        put(record, 12, 45, 2)
        put(record, 14, 45, 2)
        put(record, 24, 3, 8)
        put(record, 32, 3, 8)
        put(record, 40, number(original, end + 12, 4), 8)
        put(record, 48, number(original, end + 16, 4), 8)
        val locator = ByteArray(20)
        put(locator, 0, 0x07064b50L, 4)
        put(locator, 8, end.toLong(), 8)
        put(locator, 16, 1, 4)
        val patchedEnd = original.copyOfRange(end, original.size)
        put(patchedEnd, 8, 65535, 2)
        put(patchedEnd, 10, 65535, 2)
        put(patchedEnd, 12, 0xffffffffL, 4)
        put(patchedEnd, 16, 0xffffffffL, 4)
        assertEquals(5, resolve(original.copyOfRange(0, end) + record + locator + patchedEnd).size)
    }

    @Test fun `truncated and empty archives fail`() {
        assertFailsWith<IOException> { resolve(byteArrayOf()) }
        val valid = archive()
        assertFailsWith<IOException> { resolve(valid.copyOf(valid.size - 8)) }
    }

    @Test fun `encrypted entry fails safely`() {
        val bytes = archive()
        val record = bytes.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x01, 0x02), bytes.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x01, 0x02)) + 4)
            .let { second -> bytes.indexOfSequence(byteArrayOf(0x50, 0x4b, 0x01, 0x02), second + 4) }
        bytes[record + 8] = (bytes[record + 8].toInt() or 1).toByte()
        assertFailsWith<IOException> { resolve(bytes) }
    }

    @Test fun `unsupported compression method fails safely`() {
        val bytes = archive()
        val signature = byteArrayOf(0x50, 0x4b, 0x01, 0x02)
        val first = bytes.indexOfSequence(signature)
        val second = bytes.indexOfSequence(signature, first + 4)
        val film = bytes.indexOfSequence(signature, second + 4)
        bytes[film + 10] = 99.toByte()
        assertFailsWith<IOException> { resolve(bytes) }
    }

    private fun ByteArray.indexOfSequence(needle: ByteArray, from: Int = 0, before: Int = size): Int =
        (from..(before - needle.size)).first { i -> needle.indices.all { this[i + it] == needle[it] } }
}

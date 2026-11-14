package com.tmplayer.data

import java.io.IOException

fun interface ZipRange { fun read(offset: Long, length: Int): ByteArray }

data class ZipVideoEntry(val name: String, val method: Int, val dataOffset: Long, val compressedSize: Long, val size: Long)

/** Central-directory resolver for a raw byte-split ZIP. Reads only bounded ranges. */
object ZipEntries {
    private val video = setOf("mkv", "mp4", "avi", "webm", "ts", "m4v", "mov", "m2ts", "mts", "flv", "3gp")
    private const val MAX_DIRECTORY = 32L * 1024 * 1024

    fun video(size: Long, range: ZipRange): ZipVideoEntry {
        if (size < 22) throw IOException("ZIP is empty or truncated")
        fun bytes(at: Long, count: Int): ByteArray {
            if (at < 0 || count < 0 || at > size || count.toLong() > size - at) throw IOException("ZIP offset outside archive")
            return range.read(at, count).also { if (it.size != count) throw IOException("Truncated ZIP") }
        }
        val tailAt = (size - 65_557).coerceAtLeast(0)
        val tail = bytes(tailAt, (size - tailAt).toInt())
        val end = (tail.size - 22 downTo 0).firstOrNull { u32(tail, it) == 0x06054b50L && it + 22 + u16(tail, it + 20) == tail.size }
            ?: throw IOException("ZIP end record missing")
        if (u16(tail, end + 4) != 0 || u16(tail, end + 6) != 0) throw IOException("Spanned ZIP disks are unsupported")
        if (u16(tail, end + 8) != u16(tail, end + 10)) throw IOException("Inconsistent ZIP entry counts")
        var count = u16(tail, end + 10).toLong()
        var directorySize = u32(tail, end + 12)
        var directoryAt = u32(tail, end + 16)
        var directoryEnd = tailAt + end
        if (count == 65535L || directorySize == 0xffffffffL || directoryAt == 0xffffffffL) {
            if (end < 20) throw IOException("ZIP64 locator missing")
            val locator = bytes(tailAt + end - 20, 20)
            if (u32(locator, 0) != 0x07064b50L || u32(locator, 4) != 0L || u32(locator, 16) != 1L) throw IOException("ZIP64 locator missing or spanned")
            val recordAt = u64(locator, 8)
            directoryEnd = recordAt
            val recordHead = bytes(recordAt, 12)
            val recordLength = u64(recordHead, 4)
            if (u32(recordHead, 0) != 0x06064b50L || recordLength < 44L || recordLength > size - recordAt - 12) throw IOException("Invalid ZIP64 end record")
            if (recordAt + 12 + recordLength != tailAt + end - 20) throw IOException("ZIP64 end record overlaps locator")
            val record = bytes(recordAt, 56)
            if (u32(record, 16) != 0L || u32(record, 20) != 0L || u64(record, 24) != u64(record, 32)) throw IOException("Inconsistent ZIP64 entry counts")
            count = u64(record, 32)
            directorySize = u64(record, 40)
            directoryAt = u64(record, 48)
        }
        if (count <= 0 || count > 100_000 || directorySize > MAX_DIRECTORY || directoryAt < 0 || directoryAt > size || directorySize > size - directoryAt || directoryAt + directorySize > directoryEnd) throw IOException("Invalid ZIP directory")
        val directory = bytes(directoryAt, directorySize.toInt())
        var at = 0
        val entries = ArrayList<ZipVideoEntry>()
        var unsupported: String? = null
        repeat(count.toInt()) {
            if (at > directory.size - 46 || u32(directory, at) != 0x02014b50L) throw IOException("Invalid ZIP directory entry")
            val flags = u16(directory, at + 8)
            val method = u16(directory, at + 10)
            val nameLength = u16(directory, at + 28)
            val extraLength = u16(directory, at + 30)
            val commentLength = u16(directory, at + 32)
            val next = at.toLong() + 46 + nameLength + extraLength + commentLength
            if (next > directory.size) throw IOException("Truncated ZIP directory entry")
            val nameBytes = directory.copyOfRange(at + 46, at + 46 + nameLength)
            val name = String(nameBytes, if (flags and 0x800 != 0) Charsets.UTF_8 else charset("IBM437"))
            if (u16(directory, at + 34) != 0) throw IOException("Spanned ZIP entry is unsupported")
            var compressed = u32(directory, at + 20)
            var uncompressed = u32(directory, at + 24)
            var localAt = u32(directory, at + 42)
            if (compressed == 0xffffffffL || uncompressed == 0xffffffffL || localAt == 0xffffffffL) {
                var x = at + 46 + nameLength
                val extraEnd = x + extraLength
                var found = false
                while (x + 4 <= extraEnd) {
                    val length = u16(directory, x + 2)
                    if (x + 4 + length > extraEnd) throw IOException("Invalid ZIP extra field")
                    if (u16(directory, x) == 1) {
                        var p = x + 4
                        fun take(): Long { if (p + 8 > x + 4 + length) throw IOException("ZIP64 value missing"); return u64(directory, p).also { p += 8 } }
                        if (uncompressed == 0xffffffffL) uncompressed = take()
                        if (compressed == 0xffffffffL) compressed = take()
                        if (localAt == 0xffffffffL) localAt = take()
                        found = true
                    }
                    x += 4 + length
                }
                if (!found) throw IOException("ZIP64 metadata missing")
            }
            if (name.substringAfterLast('.', "").lowercase() in video && !name.endsWith('/') && !name.endsWith('\\')) {
                if (flags and 0x41 != 0 || (method != 0 && method != 8)) {
                    unsupported = if (flags and 0x41 != 0) "Encrypted ZIP video is unsupported" else "ZIP compression method $method is unsupported"
                    at = next.toInt()
                    return@repeat
                }
                val header = bytes(localAt, 30)
                if (u32(header, 0) != 0x04034b50L || u16(header, 8) != method) throw IOException("Invalid ZIP local header")
                val localNameLength = u16(header, 26)
                val localFlags = u16(header, 6)
                if (localFlags != flags || localNameLength != nameLength ||
                    !bytes(localAt + 30, localNameLength).contentEquals(nameBytes))
                    throw IOException("ZIP local filename differs from directory")
                val dataAt = localAt + 30L + localNameLength + u16(header, 28)
                if (dataAt < localAt || compressed < 0 || uncompressed < 0 || dataAt > size || compressed > size - dataAt) throw IOException("ZIP video extends outside archive")
                if (dataAt + compressed > directoryAt) throw IOException("ZIP video overlaps central directory")
                if (flags and 8 == 0) {
                    var localCompressed = u32(header, 18)
                    var localSize = u32(header, 22)
                    if (localCompressed == 0xffffffffL || localSize == 0xffffffffL) {
                        val extra = bytes(localAt + 30 + localNameLength, u16(header, 28))
                        var x = 0
                        var found = false
                        while (x + 4 <= extra.size) {
                            val length = u16(extra, x + 2)
                            if (length > extra.size - x - 4) throw IOException("Truncated local ZIP64 extra field")
                            if (u16(extra, x) == 1) {
                                var p = x + 4
                                fun take(): Long {
                                    if (p + 8 > x + 4 + length) throw IOException("Local ZIP64 size missing")
                                    return u64(extra, p).also { p += 8 }
                                }
                                if (localSize == 0xffffffffL) localSize = take()
                                if (localCompressed == 0xffffffffL) localCompressed = take()
                                found = true
                                break
                            }
                            x += 4 + length
                        }
                        if (!found) throw IOException("Local ZIP64 metadata missing")
                    }
                    if (u32(header, 14) != u32(directory, at + 16) ||
                        localCompressed != compressed || localSize != uncompressed)
                        throw IOException("ZIP local sizes differ from directory")
                } else {
                    val descriptorAt = dataAt + compressed
                    val descriptorLength = if (u32(directory, at + 20) == 0xffffffffL || u32(directory, at + 24) == 0xffffffffL || u32(header, 18) == 0xffffffffL || u32(header, 22) == 0xffffffffL) 20 else 12
                    fun matches(signature: Boolean): Boolean {
                        val offset = if (signature) 4 else 0
                        if (descriptorAt > directoryAt - descriptorLength - offset) return false
                        val descriptor = try { bytes(descriptorAt, descriptorLength + offset) } catch (_: IOException) { return false }
                        return (!signature || u32(descriptor, 0) == 0x08074b50L) &&
                            u32(descriptor, offset) == u32(directory, at + 16) &&
                            (if (descriptorLength == 20) u64(descriptor, offset + 4) == compressed && u64(descriptor, offset + 12) == uncompressed
                            else u32(descriptor, offset + 4) == compressed && u32(descriptor, offset + 8) == uncompressed)
                    }
                    if (!matches(false) && !matches(true)) throw IOException("ZIP data descriptor differs from directory")
                }
                if (method == 0 && compressed != uncompressed) throw IOException("Stored ZIP video has mismatched sizes")
                entries += ZipVideoEntry(name, method, dataAt, compressed, uncompressed)
            }
            at = next.toInt()
        }
        if (at != directory.size) throw IOException("ZIP directory size does not match entry records")
        return entries.sortedWith(compareByDescending<ZipVideoEntry> { it.method == 0 }.thenByDescending { it.size }.thenBy { it.name.lowercase() }.thenBy { it.dataOffset }).firstOrNull()
            ?: throw IOException(unsupported ?: "No playable video in ZIP")
    }

    private fun u16(b: ByteArray, p: Int) = (b[p].toInt() and 255) or ((b[p + 1].toInt() and 255) shl 8)
    private fun u32(b: ByteArray, p: Int): Long = u16(b, p).toLong() or (u16(b, p + 2).toLong() shl 16)
    private fun u64(b: ByteArray, p: Int): Long {
        if (p < 0 || p > b.size - 8) throw IOException("Truncated ZIP64 value")
        val high = u32(b, p + 4)
        if (high > 0x7fffffffL) throw IOException("ZIP64 value too large")
        return u32(b, p) or (high shl 32)
    }
}

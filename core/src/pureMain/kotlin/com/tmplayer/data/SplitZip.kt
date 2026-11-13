package com.tmplayer.data

/** Raw byte volumes made by splitting a ZIP file, such as `Film.zip.001`. */
data class ZipVolume(val file: ArchiveFile, val base: String, val number: Int) {
    val key: Pair<Long, String> get() = file.chatId to base.trim().lowercase()
}

data class SplitZipArchive(val volumes: List<ZipVolume>, val missing: List<Int>, val duplicates: List<Int>) {
    val complete: Boolean get() = volumes.isNotEmpty() && volumes.first().number == 1 &&
        volumes.last().number <= 100_000 && missing.isEmpty() && duplicates.isEmpty()
}

object SplitZip {
    private val suffix = Regex("^(.+\\.zip)\\.(\\d{3,})$", RegexOption.IGNORE_CASE)

    fun parse(file: ArchiveFile): ZipVolume? {
        if (file.kind != ArchiveKind.Zip) return null
        val match = suffix.matchEntire(file.fileName.trim()) ?: return null
        val number = match.groupValues[2].toIntOrNull() ?: return null
        return ZipVolume(file, match.groupValues[1], number)
    }

    fun group(selected: ZipVolume, files: List<ArchiveFile>): SplitZipArchive {
        val all = (files + selected.file).distinctBy { it.chatId to it.messageId }
            .mapNotNull(::parse).filter { it.key == selected.key }.sortedWith(compareBy<ZipVolume> { it.number }.thenBy { it.file.messageId })
        val duplicate = all.groupingBy { it.number }.eachCount().filterValues { it > 1 }.keys.sorted()
        val numbers = all.map { it.number }.toSet()
        val last = numbers.maxOrNull() ?: 0
        val missing = if (last > 100_000) listOf(1) else (1..maxOf(last, 1)).filter { it !in numbers }
        return SplitZipArchive(all, missing, duplicate)
    }
}

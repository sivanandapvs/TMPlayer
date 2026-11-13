package com.tmplayer.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.tmplayer.data.ZipRange
import com.tmplayer.data.ZipVideoEntry
import java.io.EOFException
import java.io.IOException

/** A Stored ZIP entry exposed as a standalone, seekable Media3 video. */
fun zipVideoUri(archive: Uri, entry: ZipVideoEntry): Uri = Uri.Builder().scheme("zipvideo")
    .authority("entry").appendPath(entry.name.substringAfterLast('/'))
    .appendQueryParameter("archive", archive.toString())
    .appendQueryParameter("offset", entry.dataOffset.toString())
    .appendQueryParameter("size", entry.size.toString()).build()

@UnstableApi
class ZipVideoDataSource(private val archiveFactory: DataSource.Factory) : DataSource {
    private val listeners = ArrayList<TransferListener>()
    private var source: DataSource? = null
    private var uri: Uri? = null
    private var remaining = 0L

    override fun addTransferListener(transferListener: TransferListener) { listeners += transferListener }
    override fun getUri(): Uri? = uri
    override fun getResponseHeaders(): Map<String, List<String>> =
        if (uri?.scheme != "zipvideo") source?.responseHeaders ?: emptyMap() else
            uri?.lastPathSegment?.let { streamType("", it) }?.let { mapOf("Content-Type" to listOf(it)) } ?: emptyMap()

    override fun open(dataSpec: DataSpec): Long {
        close()
        val target = dataSpec.uri
        if (target.scheme != "zipvideo") {
            val opened = archiveFactory.createDataSource()
            try {
                listeners.forEach(opened::addTransferListener)
                val length = opened.open(dataSpec)
                source = opened
                uri = target
                remaining = -1
                return length
            } catch (error: Exception) { opened.close(); throw error }
        }
        val archive = target.getQueryParameter("archive")?.let(Uri::parse) ?: throw IOException("ZIP archive URI missing")
        val offset = target.getQueryParameter("offset")?.toLongOrNull() ?: throw IOException("ZIP entry offset missing")
        val size = target.getQueryParameter("size")?.toLongOrNull() ?: throw IOException("ZIP entry size missing")
        if (offset < 0 || size < 0 || dataSpec.position < 0 || dataSpec.position > size)
            throw DataSourceException(androidx.media3.common.PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) size - dataSpec.position else minOf(dataSpec.length, size - dataSpec.position)
        if (remaining == 0L) { uri = target; return 0 }
        val opened = archiveFactory.createDataSource()
        try {
            listeners.forEach(opened::addTransferListener)
            opened.open(DataSpec.Builder().setUri(archive).setPosition(Math.addExact(offset, dataSpec.position)).setLength(remaining).build())
            source = opened
            uri = target
            return remaining
        } catch (error: Exception) {
            opened.close()
            throw error
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (uri?.scheme != "zipvideo") return source?.read(buffer, offset, length) ?: throw IOException("Source is closed")
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val read = source?.read(buffer, offset, minOf(length.toLong(), remaining).toInt()) ?: throw IOException("ZIP video is closed")
        if (read == C.RESULT_END_OF_INPUT) throw EOFException("ZIP video ended before its declared size")
        remaining -= read
        return read
    }

    override fun close() { source?.close(); source = null; uri = null; remaining = 0 }

    class Factory(private val archiveFactory: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = ZipVideoDataSource(archiveFactory)
    }
}

/** Small parser reads are opened and closed independently, preserving TDLib's range lifecycle. */
@UnstableApi
fun zipRange(factory: DataSource.Factory, archive: Uri): ZipRange = ZipRange { at, length ->
    val source = factory.createDataSource()
    try {
        source.open(DataSpec.Builder().setUri(archive).setPosition(at).setLength(length.toLong()).build())
        ByteArray(length).also { out ->
            var read = 0
            while (read < length) {
                val count = source.read(out, read, length - read)
                if (count <= 0) throw EOFException("Truncated ZIP at $at")
                read += count
            }
        }
    } finally { source.close() }
}

package com.tmplayer.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.tmplayer.data.ZipVideoEntry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ZipVideoDataSourceTest {
    private val archive = byteArrayOf(9, 9, 1, 2, 3, 4, 5, 8, 8)

    @Test fun `entry ranges stop at video end and seek across seam`() {
        // The piece factory uses the joined URI's requested part in the real implementation.
        val direct = ZipVideoDataSource(DataSource.Factory { ByteArrayDataSource(archive) })
        val flat = zipVideoUri(Uri.parse("memory://archive"), ZipVideoEntry("film.mkv", 0, 2, 5, 5))
        assertEquals(5L, direct.open(DataSpec(flat)))
        val bytes = ByteArray(20)
        assertEquals(5, direct.read(bytes, 0, bytes.size))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), bytes.copyOf(5))
        assertEquals(C.RESULT_END_OF_INPUT, direct.read(bytes, 0, bytes.size))
        direct.close()
        assertEquals(2L, direct.open(DataSpec.Builder().setUri(flat).setPosition(2).setLength(2).build()))
        assertEquals(2, direct.read(bytes, 0, bytes.size))
        assertArrayEquals(byteArrayOf(3, 4), bytes.copyOf(2))
        direct.close()
    }

    @Test fun `video bytes cross two ZIP volumes without exposing their padding`() {
        val first = Uri.parse("tdfile://1")
        val second = Uri.parse("tdfile://2")
        val joined = joinedUri(listOf(first to 4L, second to 5L))
        val pieceFactory = DataSource.Factory {
            object : DataSource {
                private var inner: ByteArrayDataSource? = null
                override fun open(dataSpec: DataSpec): Long {
                    val bytes = if (dataSpec.uri == first) archive.copyOfRange(0, 4) else archive.copyOfRange(4, 9)
                    return ByteArrayDataSource(bytes).also { inner = it }.open(dataSpec)
                }
                override fun read(buffer: ByteArray, offset: Int, length: Int) = inner!!.read(buffer, offset, length)
                override fun getUri(): Uri? = inner?.uri
                override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()
                override fun addTransferListener(listener: TransferListener) = Unit
                override fun close() { inner?.close(); inner = null }
            }
        }
        val factory = JoinedDataSource.Factory(DataSource.Factory { ByteArrayDataSource(byteArrayOf()) }, pieceFactory)
        val source = ZipVideoDataSource(factory)
        val uri = zipVideoUri(joined, ZipVideoEntry("film.mkv", 0, 2, 5, 5))
        assertEquals(5L, source.open(DataSpec(uri)))
        val bytes = ByteArray(10)
        assertEquals(2, source.read(bytes, 0, 2))
        assertEquals(3, source.read(bytes, 2, 8))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), bytes.copyOf(5))
        source.close()
    }

    @Test fun `failed archive open closes its source`() {
        var closed = false
        val source = ZipVideoDataSource(DataSource.Factory {
            object : DataSource {
                override fun open(dataSpec: DataSpec): Long = throw IOException("broken volume")
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
                override fun getUri(): Uri? = null
                override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()
                override fun addTransferListener(listener: TransferListener) = Unit
                override fun close() { closed = true }
            }
        })
        val uri = zipVideoUri(Uri.parse("memory://archive"), ZipVideoEntry("film.mkv", 0, 0, 5, 5))
        assertThrows(IOException::class.java) { source.open(DataSpec(uri)) }
        assertEquals(true, closed)
    }
}
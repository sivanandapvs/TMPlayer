package com.tmplayer.torbox

import com.tmplayer.online.HttpResponse
import com.tmplayer.online.HttpTransport
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.*

class TorBoxScaleTest {
    @Test fun twentyThousandFilesUseOneBoundedRequestAndOneFirstPageSnapshot() = runTest {
        val rows = (1..100).joinToString(",") { torrent ->
            val files = (0 until 200).joinToString(",") { file ->
                """{"id":$file,"name":"Example.$torrent.S01E$file.1080p.mkv","size":1000}"""
            }
            """{"id":$torrent,"download_finished":true,"files":[$files]}"""
        }
        val body = """{"success":true,"data":[$rows]}""".toByteArray()
        var calls = 0
        val api = TorBoxApi(HttpTransport { request ->
            calls++
            assertTrue(request.url.contains("limit=100"))
            HttpResponse(200, body = body)
        })
        lateinit var page: TorBoxPage
        val elapsed = measureTimeMillis { page = api.library("test-key-123") }
        assertEquals(1, calls)
        assertEquals(20_000, page.files.size)
        assertEquals(20_000, page.files.distinctBy { it.key }.size)
        assertEquals(100, page.nextOffset)
        val snapshot = TorBoxApi.cachePage(page, "owner", TorBoxSource.Torrents)
        assertTrue(snapshot.length < 8 * 1024 * 1024)
        assertEquals(page, TorBoxApi.cachedPage(snapshot, "owner", TorBoxSource.Torrents))
        println("TorBox: 20,000 files, one request, ${body.size} response bytes, ${elapsed}ms parse; snapshot ${snapshot.length} characters")
    }
}

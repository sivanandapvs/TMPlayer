package com.tmplayer.torbox

import com.tmplayer.online.HttpRequest
import com.tmplayer.online.HttpResponse
import com.tmplayer.online.HttpTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TorBoxApiTest {
    private val key = "private-key-123"
    private class Fake(private val answer: HttpResponse) : HttpTransport {
        val calls = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse { calls += request; return answer }
    }
    private fun response(text: String) = HttpResponse(200, body = text.toByteArray())

    @Test fun completeLibraryFollowsServerLimitedPagesAndKeepsEveryFile() = runTest {
        val offsets = mutableListOf<Int>()
        val api = TorBoxApi(HttpTransport { request ->
            val offset = Regex("offset=(\\d+)").find(request.url)!!.groupValues[1].toInt()
            offsets += offset
            val rows = (offset until minOf(offset + 2, 5)).joinToString(",") { id ->
                """{"id":$id,"download_finished":true,"files":[{"id":1,"name":"Movie.$id.MKV"},{"id":2,"name":"Movie.$id.WMV"},{"id":3,"name":"notes.zip"}]}"""
            }
            response("""{"success":true,"data":[$rows]}""")
        })
        val result = api.completeLibrary(key)
        assertEquals(listOf(0, 2, 4, 5), offsets)
        assertEquals(10, result.files.size)
        assertEquals(10, result.files.map { it.key }.distinct().size)
        val cached = TorBoxApi.cachedPage(TorBoxApi.cachePage(result, "owner", TorBoxSource.Torrents), "owner", TorBoxSource.Torrents)
        assertEquals(result.files.toSet(), cached!!.files.toSet())
    }

    @Test fun oneTorrentKeepsSeparateMoviesAndEpisodeQualityIdentities() {
        val root = TorBoxApi.parse("""{"data":[{"id":42,"download_finished":true,"files":[
            {"id":7,"name":"First.Movie.2024.1080p.mkv"},
            {"id":8,"name":"Second.Movie.2025.720p.mp4"},
            {"id":9,"name":"Show.Name.S01E02.720p.mkv"},
            {"id":10,"name":"Show.Name.S01E02.1080p.mkv"},
            {"id":10,"name":"Show.Name.S01E02.1080p.mkv"},
            {"id":11,"name":"archive.zip"}
        ]}]}""")!!
        val files = TorBoxApi.parsePage(root, 0)!!.files
        assertEquals(setOf(7L, 8L, 9L, 10L), files.map { it.fileId }.toSet())
        assertEquals(4, files.size)
        val items = files.map { it.media().toMediaItem() }
        assertEquals(4, items.map { it.id }.toSet().size)
        assertEquals(2, items.count { it.parsedName.episode == 2 && it.parsedName.season == 1 })
        assertTrue(items.all { (it.identity as com.tmplayer.data.MediaIdentity.TorBox).transferId == 42L })
    }

    @Test fun processingMetadataIsReportedWithoutClaimingAnEmptyLibrary() {
        val root = TorBoxApi.parse("""{"data":[{"id":42,"download_finished":false,"download_state":"metaDL"}]}""")!!
        val page = TorBoxApi.parsePage(root, 0)!!
        assertEquals(1, page.recordCount)
        assertEquals(setOf(42L), page.pendingTorrentIds)
        assertTrue(page.files.isEmpty())
    }

    @Test fun libraryUsesBoundedPagesAndPreservesRealFileIds() = runTest {
        val fake = Fake(response("""{"success":true,"data":[{"id":42,"download_finished":true,"created_at":"2026-11-01","files":[{"id":0,"name":"Example.S01E02.1080p.mkv","size":5000},null,{"id":1,"name":"readme.txt"},{"id":{},"name":"broken.mp4"}]}]}"""))
        val page = TorBoxApi(fake).library(key)
        assertEquals(1, page.files.size)
        val file = page.files.single()
        assertEquals("torrents:42:0", file.key)
        assertTrue(file.ready)
        assertEquals(2, file.media().parsedName.episode)
        assertNull(page.nextOffset)
        assertTrue(fake.calls.single().url.endsWith("offset=0&limit=100&bypass_cache=false"))
        assertFalse(fake.calls.single().url.contains(key))
        assertEquals("Bearer $key", fake.calls.single().headers["Authorization"])
    }

    @Test fun aFullPageOffersAnExplicitContinuation() {
        val torrents = (0 until 100).joinToString(",") { "{\"id\":$it,\"files\":[]}" }
        val root = TorBoxApi.parse("{\"data\":[$torrents]}") ?: fail()
        assertEquals(200, TorBoxApi.parsePage(root, 100)?.nextOffset)
        assertNull(TorBoxApi.parsePage(root, 99_900)?.nextOffset)
    }

    @Test fun expiredAuthenticationAndRateLimitNeverLeakTheResponseOrRetryForever() = runTest {
        for (code in listOf(401, 403, 429, 500)) {
            val fake = Fake(HttpResponse(code, mapOf("Retry-After" to "90"), "secret $key".toByteArray()))
            val error = assertFailsWith<TorBoxFailure> { TorBoxApi(fake).account(key) }
            assertFalse(error.toString().contains(key))
            assertEquals(1, fake.calls.size)
            assertEquals(90L, error.retryAfterSeconds)
        }
    }

    @Test fun streamLinksAreValidatedAndTheDocumentedParametersAreUsed() = runTest {
        val fake = Fake(response("""{"success":true,"data":"https://cdn.torbox.app/video.mkv?token=signed"}"""))
        assertTrue(TorBoxApi(fake).stream(key, 42, 0).startsWith("https://"))
        assertTrue(fake.calls.single().url.contains("file_id=0&zip_link=false&redirect=false"))
        for (url in listOf("http://cdn/video", "file:///etc/passwd", "https://user:pass@cdn/video", "https://", "https://cdn/\nfile")) {
            assertFalse(TorBoxApi.safeStreamUrl(url), url)
        }
    }

    @Test fun malformedOrFailedResponsesAreNotAnEmptySuccessfulSync() = runTest {
        for (text in listOf("{", "[]", "{\"success\":true,\"data\":{}}", "{\"success\":false,\"data\":[]}")) {
            assertFailsWith<TorBoxFailure> { TorBoxApi(Fake(response(text))).library(key) }
        }
    }

    @Test fun cancellationPropagatesAndInvalidKeysMakeNoNetworkCall() = runTest {
        val cancel = TorBoxApi(HttpTransport { throw CancellationException("cancel") })
        assertFailsWith<CancellationException> { cancel.account(key) }
        val fake = Fake(response("{}"))
        assertFailsWith<TorBoxFailure> { TorBoxApi(fake).account("bad\nkey") }
        assertTrue(fake.calls.isEmpty())
    }

    @Test fun everyCloudSourceUsesItsDocumentedIdentityParameter() = runTest {
        for (source in TorBoxSource.entries) {
            val stream = Fake(response("""{"success":true,"data":"https://cdn.torbox.app/video.mp4"}"""))
            TorBoxApi(stream).stream(key, 7, 2, source)
            assertTrue(stream.calls.single().url.contains("/${source.path}/requestdl?"))
            assertTrue(stream.calls.single().url.contains("&${source.idParameter}=7&file_id=2"))
            val list = Fake(response("""{"success":true,"data":[{"id":7,"download_finished":true,"files":[{"id":2,"name":"Example.mp4"}]}]}"""))
            val page = TorBoxApi(list).library(key, source = source)
            assertTrue(list.calls.single().url.contains("/${source.path}/mylist?"))
            assertEquals(source, page.files.single().source)
            assertEquals("${source.path}:7:2", page.files.single().key)
        }
    }

    @Test fun aRequestDeadlineBecomesARecoverableNetworkFailure() = runTest {
        var calls = 0
        val api = TorBoxApi(HttpTransport {
            calls++
            kotlinx.coroutines.delay(120_000)
            response("{}")
        })
        val failure = assertFailsWith<TorBoxFailure> { api.account(key) }
        assertEquals("network", failure.reason)
        assertEquals(1, calls)
    }

    @Test fun absurdJsonNestingIsRejectedWithoutRejectingBracesInFileNames() {
        assertNull(TorBoxApi.parse("{\"nested\":".repeat(10_000) + "0" + "}".repeat(10_000)))
        assertNotNull(TorBoxApi.parse("""{"name":"${"{".repeat(1000)}","data":[]}"""))
    }
}

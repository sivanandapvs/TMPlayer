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
}

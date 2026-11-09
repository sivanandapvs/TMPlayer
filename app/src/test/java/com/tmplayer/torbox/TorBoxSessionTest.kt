package com.tmplayer.torbox

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.tmplayer.online.HttpRequest
import com.tmplayer.online.HttpResponse
import com.tmplayer.online.HttpTransport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class TorBoxSessionTest {
    private val models = mutableListOf<TorBoxViewModel>()
    private val token = "test-account-key"

    private class MemoryVault : TorBoxVault {
        @Volatile var key: String? = null
        val writes = CopyOnWriteArrayList<String>()
        val pages = ConcurrentHashMap<Pair<String, TorBoxSource>, TorBoxPage>()
        override fun read() = key
        override fun write(key: String) { this.key = key; writes += key }
        override fun clear() { key = null; pages.clear() }
        override fun readLibrary(key: String, source: TorBoxSource) = pages[key to source]
        override fun writeLibrary(key: String, source: TorBoxSource, page: TorBoxPage): Boolean {
            pages[key to source] = page
            return true
        }
    }

    private class Service : HttpTransport {
        val calls = CopyOnWriteArrayList<HttpRequest>()
        @Volatile var rejectKey: String? = null
        @Volatile var failLibrary = false
        var accountGate: CompletableDeferred<Unit>? = null
        override suspend fun send(request: HttpRequest): HttpResponse {
            calls += request
            if (request.url.contains("/user/me?")) {
                accountGate?.await()
                if (request.headers["Authorization"] == "Bearer $rejectKey") return HttpResponse(401)
                return ok("""{"success":true,"data":{"email":"demo@example.invalid","plan":2}}""")
            }
            if (failLibrary) return HttpResponse(503)
            return ok("""{"success":true,"data":[{"id":7,"download_finished":true,"files":[{"id":0,"name":"Example.mp4","size":5000}]}]}""")
        }
        private fun ok(text: String) = HttpResponse(200, body = text.toByteArray())
    }

    private fun model(service: Service, vault: MemoryVault) = TorBoxViewModel(
        ApplicationProvider.getApplicationContext(), TorBoxApi(service), vault,
    ).also { models += it }

    private fun awaitState(predicate: () -> Boolean) {
        val until = System.nanoTime() + 5_000_000_000L
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            Thread.sleep(10)
        } while (System.nanoTime() < until)
        fail("TorBox state did not settle within five seconds")
    }

    @After fun closeModels() {
        models.forEach { ReflectionHelpers.callInstanceMethod<Any>(it, "onCleared") }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun connectionValidatesOnceAndAnInvalidReplacementKeepsTheWorkingAccount() {
        val service = Service()
        val vault = MemoryVault()
        val model = model(service, vault)
        awaitState { !model.state.value.busy }
        model.connect(token) {}
        awaitState { model.state.value.connected && !model.state.value.busy }
        assertEquals(1, service.calls.count { it.url.contains("/user/me?") })
        assertEquals(1, service.calls.count { it.url.contains("/mylist?") })
        assertEquals(listOf(token), vault.writes)
        val library = model.state.value.files

        service.rejectKey = "invalid-account-key"
        model.connect("invalid-account-key") {}
        awaitState { model.state.value.error == "auth" && !model.state.value.busy }
        assertTrue(model.state.value.connected)
        assertEquals(token, vault.key)
        assertEquals(listOf(token), vault.writes)
        assertEquals(library, model.state.value.files)
    }

    @Test fun aFailedRefreshKeepsTheLibraryAndDisconnectNeedsNoRemoteEndpoint() {
        val service = Service()
        val vault = MemoryVault()
        val model = model(service, vault)
        awaitState { !model.state.value.busy }
        model.connect(token) {}
        awaitState { model.state.value.files.isNotEmpty() && !model.state.value.busy }
        val library = model.state.value.files
        service.failLibrary = true
        model.refresh()
        awaitState { model.state.value.error == "network" && !model.state.value.busy }
        assertEquals(library, model.state.value.files)
        val calls = service.calls.size
        model.disconnect()
        awaitState { !model.state.value.connected && !model.state.value.busy }
        assertNull(vault.key)
        assertTrue(vault.pages.isEmpty())
        assertTrue(model.state.value.files.isEmpty())
        assertEquals(calls, service.calls.size)
    }

    @Test fun theSavedFirstPageAppearsBeforeTheNetworkAnswers() {
        val service = Service().apply { accountGate = CompletableDeferred() }
        val cached = TorBoxFile(1, 0, "Saved.example.mp4", 1000, "", true)
        val vault = MemoryVault().apply {
            key = token
            pages[token to TorBoxSource.Torrents] = TorBoxPage(listOf(cached), null)
        }
        val model = model(service, vault)
        awaitState { model.state.value.fromCache }
        assertTrue(model.state.value.busy)
        assertEquals(listOf(cached), model.state.value.files)
        service.accountGate?.complete(Unit)
        awaitState { !model.state.value.busy }
        assertFalse(model.state.value.fromCache)
        assertEquals("Example.mp4", model.state.value.files.singleOrNull()?.name)
        assertEquals(2, service.calls.size)
    }
}

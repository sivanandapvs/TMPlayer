package com.tmplayer.online

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrlConnectionCancellationTest {
    @Test(timeout = 15_000)
    fun cancellingClosesARequestBlockedBeforeHeadersArrive() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor { task -> Thread(task).apply { isDaemon = true } }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            this.executor = executor
            createContext("/waiting") { exchange ->
                started.complete(Unit)
                try { release.await(10, TimeUnit.SECONDS) } finally { exchange.close() }
            }
            start()
        }
        val job = async(Dispatchers.Default) {
            UrlConnectionTransport.send(HttpRequest("GET", "http://127.0.0.1:${server.address.port}/waiting"))
        }
        try {
            withTimeout(5_000) { started.await() }
            withTimeout(3_000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
        } finally {
            release.countDown()
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test(timeout = 10_000)
    fun successfulResponsesKeepTheirConnectionForReuse() = runBlocking {
        val ports = CopyOnWriteArrayList<Int>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/ready") { exchange ->
                ports += exchange.remoteAddress.port
                val body = "ready".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        try {
            repeat(2) {
                val response = withTimeout(5_000) {
                    UrlConnectionTransport.send(HttpRequest("GET", "http://127.0.0.1:${server.address.port}/ready"))
                }
                assertEquals(200, response.code)
                assertEquals("ready", response.text)
            }
            assertEquals(2, ports.size)
            assertEquals(1, ports.distinct().size)
        } finally { server.stop(0) }
    }
}

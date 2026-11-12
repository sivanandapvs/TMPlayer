package com.tmplayer.torbox

import com.tmplayer.json.JSONObject
import com.tmplayer.json.JSONArray
import com.tmplayer.online.HttpRequest
import com.tmplayer.online.HttpTransport
import com.tmplayer.platform.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay

enum class TorBoxSource(val path: String, val idParameter: String) {
    Torrents("torrents", "torrent_id"),
    Usenet("usenet", "usenet_id"),
    Web("webdl", "web_id"),
}

/** Wire records only. Screens and playback use the existing MediaItem model. */
data class TorBoxFile(
    val torrentId: Long,
    val fileId: Long,
    val name: String,
    val size: Long,
    val added: String,
    val ready: Boolean,
    val source: TorBoxSource = TorBoxSource.Torrents,
) {
    val key: String get() = "${source.path}:$torrentId:$fileId"
}

data class TorBoxPage(
    val files: List<TorBoxFile>,
    val nextOffset: Int?,
    val recordCount: Int = 0,
    val pendingTorrentIds: Set<Long> = emptySet(),
)
data class TorBoxAccount(val email: String, val plan: String, val expires: String)

/** Deliberately excludes provider bodies and URLs: they may contain credentials. */
class TorBoxFailure(val reason: String, val status: Int = 0, val retryAfterSeconds: Long = 0) :
    IOException("TorBox: $reason")

/** Official /v1/api contract. No background polling, automatic retries or key logging. */
class TorBoxApi(private val http: HttpTransport) {
    suspend fun account(key: String): TorBoxAccount {
        val data = get(key, "/user/me?settings=false").optJSONObject("data")
            ?: throw TorBoxFailure("response")
        return TorBoxAccount(data.optString("email").take(320), data.optString("plan").take(64),
            data.optString("premium_expires_at").take(64))
    }

    suspend fun library(key: String, offset: Int = 0, refresh: Boolean = false, source: TorBoxSource = TorBoxSource.Torrents): TorBoxPage {
        if (offset !in 0..MAX_TORRENTS) throw TorBoxFailure("limit")
        val root = get(key, "/${source.path}/mylist?offset=$offset&limit=$PAGE_SIZE&bypass_cache=$refresh")
        return parsePage(root, offset, source) ?: throw TorBoxFailure("response")
    }

    /** Walk database offsets, including when the server returns fewer rows than requested. */
    suspend fun completeLibrary(key: String, source: TorBoxSource = TorBoxSource.Torrents): TorBoxPage {
        val files = LinkedHashMap<String, TorBoxFile>()
        val pending = LinkedHashSet<Long>()
        var offset = 0
        var previousKeys: Set<String>? = null
        while (offset < MAX_TORRENTS) {
            val page = libraryWithRetry(key, offset, source)
            if (page.recordCount == 0) return TorBoxPage(files.values.toList(), null, offset, pending)
            val keys = page.files.mapTo(LinkedHashSet()) { it.key }
            if (keys.isNotEmpty() && keys == previousKeys) throw TorBoxFailure("pagination")
            previousKeys = keys
            pending += page.pendingTorrentIds
            page.files.forEach { files[it.key] = it }
            offset += page.recordCount
        }
        throw TorBoxFailure("limit")
    }

    private suspend fun libraryWithRetry(key: String, offset: Int, source: TorBoxSource): TorBoxPage {
        var attempt = 0
        while (true) {
            try { return library(key, offset, refresh = true, source = source) }
            catch (failure: TorBoxFailure) {
                if (failure.status in listOf(401, 403) || failure.reason !in setOf("network", "rate") || attempt++ >= 2) throw failure
                delay(failure.retryAfterSeconds.takeIf { it > 0 }?.coerceAtMost(30)?.times(1000)
                    ?: (500L shl (attempt - 1)))
            }
        }
    }

    suspend fun stream(key: String, torrent: Long, file: Long, source: TorBoxSource = TorBoxSource.Torrents): String {
        if (torrent < 0 || file < 0) throw TorBoxFailure("response")
        // requestdl requires token in the query. The transport must never log request URLs.
        val data = get(key, "/${source.path}/requestdl?token=${encodeKey(key)}&${source.idParameter}=$torrent&file_id=$file&zip_link=false&redirect=false")
            .opt("data") as? String
        return data?.takeIf(::safeStreamUrl) ?: throw TorBoxFailure("response")
    }

    private suspend fun get(key: String, path: String): JSONObject {
        if (!validKey(key)) throw TorBoxFailure("key")
        val response = try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                http.send(HttpRequest("GET", BASE + path, mapOf("Authorization" to "Bearer $key", "Accept" to "application/json")))
            }
        } catch (_: TimeoutCancellationException) {
            throw TorBoxFailure("network")
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            throw TorBoxFailure("network")
        }
        if (response.code !in 200..299) {
            val reason = when (response.code) { 401, 403 -> "auth"; 429 -> "rate"; else -> "network" }
            throw TorBoxFailure(reason, response.code, response.header("Retry-After")?.toLongOrNull()?.coerceIn(0, 86400) ?: 0)
        }
        val root = parse(response.text) ?: throw TorBoxFailure("response")
        if (!root.optBoolean("success")) throw TorBoxFailure(
            when (root.optString("error")) {
                "BAD_TOKEN", "AUTH_ERROR", "PLAN_RESTRICTED_FEATURE" -> "auth"
                "TORRENT_NOT_FINISHED" -> "pending"
                else -> "response"
            })
        return root
    }

    companion object {
        const val BASE = "https://api.torbox.app/v1/api"
        const val PAGE_SIZE = 100
        const val MAX_TORRENTS = 100_000
        private const val REQUEST_TIMEOUT_MS = 45_000L
        private const val MAX_JSON_DEPTH = 32
        private val videoExtensions = setOf("mkv", "mp4", "m4v", "webm", "avi", "mov", "wmv", "ts", "m2ts", "mpg", "mpeg", "3gp", "flv", "ogv")

        fun validKey(key: String): Boolean = key.length in 8..512 && key.all { it.code in 33..126 }

        private fun encodeKey(key: String): String = buildString {
            key.forEach { c ->
                if (c.isLetterOrDigit() || c in "-._~") append(c)
                else append('%').append(c.code.toString(16).uppercase().padStart(2, '0'))
            }
        }

        fun safeStreamUrl(url: String): Boolean = url.length in 9..8192 &&
            url.startsWith("https://", ignoreCase = true) && url.none { it.isWhitespace() || it.code < 32 } &&
            url.substringAfter("://").substringBefore('/').let { it.isNotBlank() && '@' !in it && '\\' !in it }

        fun parse(text: String): JSONObject? {
            if (text.length > 32 * 1024 * 1024) return null
            // Bound nesting before the shared JSON wrapper constructs its recursive document.
            var depth = 0
            var quoted = false
            var escaped = false
            for (char in text) {
                if (quoted) {
                    if (escaped) escaped = false
                    else when (char) { '\\' -> escaped = true; '"' -> quoted = false }
                } else when (char) {
                    '"' -> quoted = true
                    '{', '[' -> if (++depth > MAX_JSON_DEPTH) return null
                    '}', ']' -> if (--depth < 0) return null
                }
            }
            if (depth != 0 || quoted) return null
            return try { JSONObject(text) } catch (_: Exception) { null }
        }

        fun parsePage(root: JSONObject, offset: Int, source: TorBoxSource = TorBoxSource.Torrents): TorBoxPage? {
            val torrents = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("data") ?: return null
            if (torrents.length() > PAGE_SIZE || offset !in 0..MAX_TORRENTS) return null
            val files = ArrayList<TorBoxFile>()
            val pending = LinkedHashSet<Long>()
            for (i in 0 until torrents.length()) {
                val torrent = torrents.optJSONObject(i) ?: continue
                val id = torrent.optString("id").toLongOrNull()?.takeIf { it >= 0 } ?: continue
                val added = torrent.optString("created_at").take(64)
                val ready = torrent.optBoolean("download_finished") ||
                    torrent.optString("download_state", torrent.optString("status")) in setOf("completed", "seeding", "cached")
                val entries = torrent.optJSONArray("files")
                if (!ready || entries == null) pending += id
                if (entries == null) continue
                for (j in 0 until minOf(entries.length(), 20_000)) {
                    if (files.size >= 20_000) break
                    val file = entries.optJSONObject(j) ?: continue
                    val fileId = file.optString("id").toLongOrNull()?.takeIf { it >= 0 } ?: continue
                    val name = file.opt("name") as? String ?: continue
                    if (name.length !in 1..4096 || name.substringAfterLast('.', "").lowercase() !in videoExtensions) continue
                    files += TorBoxFile(id, fileId, name, file.optLong("size").coerceAtLeast(0), added, ready, source)
                }
            }
            val next = (offset + torrents.length()).takeIf { torrents.length() == PAGE_SIZE && it < MAX_TORRENTS }
            return TorBoxPage(files.distinctBy { it.key }.sortedByDescending { it.added }, next, torrents.length(), pending)
        }

        /** A bounded first-page snapshot, using the same file parser as the network. */
        fun cachePage(page: TorBoxPage, owner: String, source: TorBoxSource): String {
            val groups = page.files.filter { it.source == source }.groupBy { it.torrentId }
            val torrents = JSONArray()
            for ((id, files) in groups) {
                val entries = JSONArray()
                files.forEach { file ->
                    entries.put(JSONObject().put("id", file.fileId).put("name", file.name).put("size", file.size))
                }
                torrents.put(JSONObject().put("id", id).put("download_finished", files.all { it.ready })
                    .put("created_at", files.firstOrNull()?.added.orEmpty()).put("files", entries))
            }
            return JSONObject().put("version", if (page.recordCount == 0 && groups.size <= PAGE_SIZE) 1 else 2)
                .put("record_count", page.recordCount).put("owner", owner).put("source", source.name)
                .put("next_offset", page.nextOffset ?: -1).put("data", torrents).toString()
        }

        fun cachedPage(text: String, owner: String, source: TorBoxSource): TorBoxPage? {
            val root = parse(text) ?: return null
            if (root.optInt("version") !in 1..2 || root.optString("owner") != owner || root.optString("source") != source.name) return null
            val rows = root.optJSONArray("data") ?: return null
            val page = if (rows.length() <= PAGE_SIZE) parsePage(root, 0, source) ?: return null else {
                val all = ArrayList<TorBoxFile>()
                for (start in 0 until rows.length() step PAGE_SIZE) {
                    val chunk = JSONArray()
                    for (index in start until minOf(start + PAGE_SIZE, rows.length())) chunk.put(rows.optJSONObject(index))
                    all += parsePage(JSONObject().put("data", chunk), start, source)?.files ?: return null
                }
                TorBoxPage(all.distinctBy { it.key }, null, rows.length())
            }
            val next = root.optInt("next_offset", -1)
            if (next != -1 && (next !in 1 until MAX_TORRENTS ||
                (root.optInt("version") == 1 && next % PAGE_SIZE != 0))) return null
            return page.copy(nextOffset = next.takeIf { it >= 0 },
                recordCount = if (root.optInt("version") == 1) 0 else root.optInt("record_count", page.recordCount))
        }
    }
}

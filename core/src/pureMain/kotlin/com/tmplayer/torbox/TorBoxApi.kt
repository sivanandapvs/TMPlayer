package com.tmplayer.torbox

import com.tmplayer.json.JSONObject
import com.tmplayer.json.JSONArray
import com.tmplayer.online.HttpRequest
import com.tmplayer.online.HttpTransport
import com.tmplayer.platform.IOException
import kotlinx.coroutines.CancellationException

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

data class TorBoxPage(val files: List<TorBoxFile>, val nextOffset: Int?)
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
            http.send(HttpRequest("GET", BASE + path, mapOf("Authorization" to "Bearer $key", "Accept" to "application/json")))
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
        private val videoExtensions = setOf("mkv", "mp4", "m4v", "webm", "avi", "mov", "ts", "m2ts", "mpg", "mpeg", "3gp")

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
            if (text.length > 8 * 1024 * 1024) return null
            return try { JSONObject(text) } catch (_: Exception) { null }
        }

        fun parsePage(root: JSONObject, offset: Int, source: TorBoxSource = TorBoxSource.Torrents): TorBoxPage? {
            val torrents = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("data") ?: return null
            if (torrents.length() > PAGE_SIZE || offset !in 0..MAX_TORRENTS) return null
            val files = ArrayList<TorBoxFile>()
            for (i in 0 until torrents.length()) {
                val torrent = torrents.optJSONObject(i) ?: continue
                val id = torrent.optString("id").toLongOrNull()?.takeIf { it >= 0 } ?: continue
                val added = torrent.optString("created_at").take(64)
                val ready = torrent.optBoolean("download_finished") ||
                    torrent.optString("download_state", torrent.optString("status")) in setOf("completed", "seeding", "cached")
                val entries = torrent.optJSONArray("files") ?: continue
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
            return TorBoxPage(files.distinctBy { it.key }.sortedByDescending { it.added }, next)
        }

        /** A bounded first-page snapshot, using the same file parser as the network. */
        fun cachePage(page: TorBoxPage, owner: String, source: TorBoxSource): String {
            val groups = page.files.filter { it.source == source }.groupBy { it.torrentId }
            val torrents = JSONArray()
            for ((id, files) in groups.entries.take(PAGE_SIZE)) {
                val entries = JSONArray()
                files.forEach { file ->
                    entries.put(JSONObject().put("id", file.fileId).put("name", file.name).put("size", file.size))
                }
                torrents.put(JSONObject().put("id", id).put("download_finished", files.all { it.ready })
                    .put("created_at", files.firstOrNull()?.added.orEmpty()).put("files", entries))
            }
            return JSONObject().put("version", 1).put("owner", owner).put("source", source.name)
                .put("next_offset", page.nextOffset ?: -1).put("data", torrents).toString()
        }

        fun cachedPage(text: String, owner: String, source: TorBoxSource): TorBoxPage? {
            val root = parse(text) ?: return null
            if (root.optInt("version") != 1 || root.optString("owner") != owner || root.optString("source") != source.name) return null
            val page = parsePage(root, 0, source) ?: return null
            val next = root.optInt("next_offset", -1)
            if (next != -1 && (next !in PAGE_SIZE until MAX_TORRENTS || next % PAGE_SIZE != 0)) return null
            return page.copy(nextOffset = next.takeIf { it >= 0 })
        }
    }
}

package com.tmplayer.torbox

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.tmplayer.data.MediaItem
import com.tmplayer.online.UrlConnectionTransport
import com.tmplayer.platform.JobFailures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The cloud half of Home's Recently added feed.
 *
 * Reads the encrypted first-page snapshots immediately, then refreshes each TorBox source in the
 * background. The Telegram Home model owns the combined chronology; this view model only owns the
 * provider boundary and never fabricates Telegram chat/message identifiers.
 */
class TorBoxHomeViewModel(app: Application) : AndroidViewModel(app) {
    private val api = TorBoxApi(UrlConnectionTransport)
    private val vault = TorBoxCredentials(app)
    private val scope = JobFailures.viewModelScope("TorBox Home")
    private val mutableItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val items = mutableItems.asStateFlow()
    private var work: Job? = null
    private var knownCredential: String? = null
    private var lastFetchAt = 0L

    init {
        refresh()
    }

    /** Called on Home resume; credential changes (connect/disconnect) bypass the refresh throttle. */
    fun refresh(force: Boolean = false) {
        if (work?.isActive == true) return
        work = scope.launch {
            try {
                val key = withContext(Dispatchers.IO) { runCatching { vault.read() }.getOrNull() }
                if (key == null) {
                    knownCredential = null
                    mutableItems.value = emptyList()
                    return@launch
                }
                // Keep only a one-way fingerprint in memory; never put the API key in state or logs.
                val fingerprint = withContext(Dispatchers.Default) {
                    java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                }
                val now = SystemClock.elapsedRealtime()
                if (!force && knownCredential == fingerprint && now - lastFetchAt < REFRESH_INTERVAL_MS) return@launch
                knownCredential = fingerprint
                lastFetchAt = now

                val merged = LinkedHashMap<String, TorBoxFile>()
                // Paint cached files before waiting for the network, grouped by their real source ID.
                TorBoxSource.entries.forEach { source ->
                    val cached = withContext(Dispatchers.IO) {
                        runCatching { vault.readLibrary(key, source)?.files.orEmpty() }.getOrDefault(emptyList())
                    }
                    cached.filter { it.ready }.forEach { merged[it.key] = it }
                }
                if (merged.isNotEmpty()) mutableItems.value = mediaItems(merged.values)

                var succeeded = false
                for (source in TorBoxSource.entries) {
                    val page = try {
                        withContext(Dispatchers.IO) { api.completeLibrary(key, source) }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        // Preserve the last encrypted page for this source if the network is down.
                        continue
                    }
                    succeeded = true
                    val stillProcessing = merged.values.filter { it.source == source && it.torrentId in page.pendingTorrentIds }
                    merged.keys.removeAll { it.startsWith("${source.path}:") }
                    page.files.filter { it.ready }.forEach { merged[it.key] = it }
                    stillProcessing.forEach { merged.putIfAbsent(it.key, it) }
                    val snapshot = page.copy(files = merged.values.filter { it.source == source })
                    withContext(Dispatchers.IO) { runCatching { vault.writeLibrary(key, source, snapshot) } }
                    mutableItems.value = mediaItems(merged.values)
                }
                // A successful empty result is meaningful (the user removed all their cloud files).
                if (succeeded) mutableItems.value = mediaItems(merged.values)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Home remains usable when the optional cloud provider cannot be read.
            }
        }
    }

    private fun mediaItems(files: Collection<TorBoxFile>): List<MediaItem> =
        files.asSequence()
            .filter { it.ready }
            .distinctBy { it.key }
            .map { file -> file.media().toMediaItem().copy(date = addedAtSeconds(file.added)) }
            .sortedWith(compareByDescending<MediaItem> { it.date }.thenByDescending { it.id })
            .toList()

    /** TorBox supplies ISO-8601 UTC timestamps; MediaItem's shared ordering uses Unix seconds. */
    private fun addedAtSeconds(value: String): Int = runCatching {
        val match = ISO_DATE.matchEntire(value.trim()) ?: return@runCatching 0
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        var millis = formatter.parse(match.groupValues[1])?.time ?: return@runCatching 0
        val zone = match.groupValues[2]
        if (zone.isNotEmpty() && zone != "Z") {
            val sign = if (zone[0] == '-') -1 else 1
            val digits = zone.substring(1).replace(":", "")
            if (digits.length == 4) {
                val minutes = digits.substring(0, 2).toInt() * 60 + digits.substring(2, 4).toInt()
                millis -= sign * minutes * 60_000L
            }
        }
        (millis / 1_000L).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }.getOrDefault(0)

    override fun onCleared() {
        work?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 3 * 60 * 1_000L
        private val ISO_DATE = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.\d+)?(Z|[+-]\d{2}:?\d{2})?$""")
    }
}

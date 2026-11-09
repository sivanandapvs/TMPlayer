package com.tmplayer.torbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tmplayer.online.UrlConnectionTransport
import com.tmplayer.platform.JobFailures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TorBoxState(
    val connected: Boolean = false,
    val busy: Boolean = false,
    val account: TorBoxAccount? = null,
    val files: List<TorBoxFile> = emptyList(),
    val nextOffset: Int? = null,
    val error: String? = null,
    val refreshed: Boolean = false,
    val source: TorBoxSource = TorBoxSource.Torrents,
)

class TorBoxViewModel(app: Application) : AndroidViewModel(app) {
    private val credentials = TorBoxCredentials(app)
    private val api = TorBoxApi(UrlConnectionTransport)
    private val scope = JobFailures.viewModelScope("TorBox")
    private val mutable = MutableStateFlow(TorBoxState())
    val state = mutable.asStateFlow()
    private var key: String? = null
    private var work: Job? = null

    init {
        run {
            key = withContext(Dispatchers.IO) { credentials.read() }
            mutable.value = mutable.value.copy(connected = key != null)
            key?.let { load(it, false) }
        }
    }

    fun connect(candidate: String, done: () -> Unit) = run {
        val trimmed = candidate.trim()
        if (!TorBoxApi.validKey(trimmed)) throw TorBoxFailure("key")
        val account = withContext(Dispatchers.Default) { api.account(trimmed) }
        // Persist only after validation. An invalid replacement never loses a working account.
        // Credential replacement is one transaction even if the screen is closed during disk I/O.
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) { credentials.write(trimmed) }
            key = trimmed
            mutable.value = TorBoxState(connected = true, busy = true, account = account)
            done()
        }
        load(trimmed, false)
    }

    fun refresh(more: Boolean = false) = run { key?.let { load(it, more) } }

    fun selectSource(source: TorBoxSource) = run {
        if (source == mutable.value.source) return@run
        mutable.value = mutable.value.copy(source = source, files = emptyList(), nextOffset = null, refreshed = false)
        key?.let { load(it, false) }
    }

    fun disconnect() = run {
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) { credentials.clear() }
            key = null
            mutable.value = TorBoxState()
        }
    }

    private suspend fun load(token: String, more: Boolean) {
        val before = mutable.value
        val offset = if (more) before.nextOffset ?: return else 0
        val account = withContext(Dispatchers.Default) {
            if (more) before.account ?: api.account(token) else api.account(token)
        }
        val page = withContext(Dispatchers.Default) { api.library(token, offset, refresh = !more, source = before.source) }
        val files = withContext(Dispatchers.Default) {
            ((if (more) before.files else emptyList()) + page.files)
                .distinctBy { it.key }.sortedByDescending { it.added }.take(MAX_FILES)
        }
        mutable.value = before.copy(connected = true, busy = true, account = account, files = files,
            nextOffset = page.nextOffset.takeIf { files.size < MAX_FILES }, refreshed = true, error = null)
    }

    private fun run(block: suspend () -> Unit) {
        if (work?.isActive == true) return
        work = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            try { block() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                mutable.value = mutable.value.copy(error = (failure as? TorBoxFailure)?.reason ?: "storage")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun cancel() { work?.cancel() }
    override fun onCleared() { scope.coroutineContext[Job]?.cancel() }

    companion object { const val MAX_FILES = 20_000 }
}

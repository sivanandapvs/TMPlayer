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
    val fromCache: Boolean = false,
)

class TorBoxViewModel internal constructor(
    app: Application,
    private val api: TorBoxApi,
    private val credentials: TorBoxVault,
) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, TorBoxApi(UrlConnectionTransport), TorBoxCredentials(app))
    private val scope = JobFailures.viewModelScope("TorBox")
    private val mutable = MutableStateFlow(TorBoxState())
    val state = mutable.asStateFlow()
    private var key: String? = null
    private var work: Job? = null

    init {
        run {
            key = withContext(Dispatchers.IO) { credentials.read() }
            val token = key
            if (token == null) {
                mutable.value = TorBoxState()
            } else {
                val account = withContext(Dispatchers.IO) { api.account(token) }
                mutable.value = TorBoxState(connected = true, account = account, refreshed = true)
            }
        }
    }

    fun connect(candidate: String, done: () -> Unit) = run {
        val trimmed = candidate.trim()
        if (!TorBoxApi.validKey(trimmed)) throw TorBoxFailure("key")
        // Validate the credential with the account endpoint before replacing a working key.
        val account = withContext(Dispatchers.IO) { api.account(trimmed) }
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) { credentials.write(trimmed) }
            key = trimmed
            mutable.value = TorBoxState(connected = true, account = account, refreshed = true)
            done()
        }
    }

    /** Re-check the account only. The media catalog is owned and refreshed by Home. */
    fun refresh() = run {
        val token = key ?: return@run
        val account = withContext(Dispatchers.IO) { api.account(token) }
        mutable.value = mutable.value.copy(connected = true, account = account, refreshed = true, error = null)
    }

    fun disconnect() = run {
        withContext(NonCancellable) {
            withContext(Dispatchers.IO) { credentials.clear() }
            key = null
            mutable.value = TorBoxState()
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (work?.isCompleted == false) return
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

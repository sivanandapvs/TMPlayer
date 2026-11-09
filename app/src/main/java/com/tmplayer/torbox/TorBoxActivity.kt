package com.tmplayer.torbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmplayer.data.FormFactor
import com.tmplayer.data.guardedLifecycleScope as lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import com.tmplayer.player.PlayerActivity
import com.tmplayer.ui.i18n.LocalStrings
import com.tmplayer.ui.settings.SendLogsButton
import com.tmplayer.ui.theme.TMPlayerTheme

/** A destination of the existing app, reachable before or after Telegram sign-in. */
class TorBoxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val lock = runCatching { com.tmplayer.data.SettingsStore(this@TorBoxActivity).appLock.first() }.getOrNull()
            if (lock == null || (lock.onOpen && lock.pinSet && !com.tmplayer.data.AppLockSession.unlocked.value)) {
                startActivity(android.content.Intent(this@TorBoxActivity, com.tmplayer.MainActivity::class.java))
                finish()
            } else setContent { TMPlayerTheme { TorBoxScreen(onBack = ::finish) } }
        }
    }

    override fun onStart() {
        super.onStart()
        if (com.tmplayer.data.AppLockSession.unlocked.value) return
        lifecycleScope.launch {
            val lock = runCatching { com.tmplayer.data.SettingsStore(this@TorBoxActivity).appLock.first() }.getOrNull()
            if (lock == null || (lock.pinSet && lock.onOpen && !com.tmplayer.data.AppLockSession.unlocked.value)) {
                startActivity(android.content.Intent(this@TorBoxActivity, com.tmplayer.MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun TorBoxScreen(onBack: () -> Unit, model: TorBoxViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val s = LocalStrings.current
    val context = LocalContext.current
    val tv = FormFactor.isTv(context)
    val first = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var focusedInitially by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var disconnecting by remember { mutableStateOf(false) }
    var launchError by remember { mutableStateOf(false) }
    fun text(key: String) = s.text("torbox.$key")
    val files by produceState(emptyList<TorBoxFile>(), state.files, search, kind) {
        if (search.isNotBlank()) delay(150)
        value = withContext(Dispatchers.Default) {
            state.files.filter { file ->
                coroutineContext.ensureActive()
                (search.isBlank() || file.name.contains(search, ignoreCase = true)) && when (kind) {
                    1 -> file.media().parsedName.episode == null
                    2 -> file.media().parsedName.episode != null
                    else -> true
                }
            }
        }
    }
    LaunchedEffect(tv, windowFocused) {
        if (tv && windowFocused && !focusedInitially) {
            repeat(3) {
                withFrameNanos { }
                if (first.requestFocus()) {
                    focusedInitially = true
                    return@LaunchedEffect
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (tv) 48.dp else 20.dp),
            contentPadding = PaddingValues(vertical = if (tv) 27.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    com.tmplayer.ui.components.BrandLockup(
                        size = com.tmplayer.ui.components.BrandSize.Small,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onBack, modifier = Modifier.focusRequester(first)) { Text(s.commonBack) }
                }
                Text(text("title"), style = MaterialTheme.typography.titleLarge)
                if (state.fromCache) Text(text("cached"), style = MaterialTheme.typography.bodyMedium)
            }
            if (state.busy) item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = model::cancel) { Text(s.commonCancel) }
            }
            if (state.error != null || launchError) item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val reason = state.error ?: "playback"
                        Text(text("error_$reason"), color = MaterialTheme.colorScheme.error)
                        if (state.connected) Button(onClick = { model.refresh() }, enabled = !state.busy) { Text(s.commonRetry) }
                        SendLogsButton()
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text(if (state.connected) "connected" else "disconnected"), style = MaterialTheme.typography.titleMedium)
                        state.account?.let { account ->
                            if (account.email.isNotBlank()) Text(account.email)
                            if (account.plan.isNotBlank()) Text(text("plan") + ": " + account.plan)
                            if (account.expires.isNotBlank()) Text(text("expires") + ": " + account.expires)
                        }
                        OutlinedTextField(
                            value = draft, onValueChange = { if (it.length <= 512) draft = it },
                            label = { Text(text("api_key")) }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                            enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { model.connect(draft) { draft = "" } }, enabled = !state.busy && draft.isNotBlank()) {
                            Text(text(if (state.connected) "replace_key" else "connect"))
                        }
                        if (state.connected || state.error == "storage") OutlinedButton(
                            onClick = { disconnecting = true }, enabled = !state.busy,
                        ) { Text(text("disconnect")) }
                    }
                }
            }
            if (state.connected) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TorBoxSource.entries.forEach { source ->
                            FilterChip(selected = state.source == source, enabled = !state.busy,
                                onClick = { model.selectSource(source) },
                                label = { Text(text("source_${source.name.lowercase()}")) })
                        }
                    }
                }
                item {
                    Button(onClick = { model.refresh() }, enabled = !state.busy) { Text(text("refresh")) }
                    Text(text("refresh_detail"), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(search, { search = it.take(256) }, label = { Text(text("search")) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("recent", "videos", "series").forEachIndexed { index, label ->
                            FilterChip(selected = kind == index, onClick = { kind = index }, label = { Text(text(label)) })
                        }
                    }
                    Text(text("versions_detail"), style = MaterialTheme.typography.bodyMedium)
                }
                if (files.isEmpty() && !state.busy && state.refreshed) item { Text(text("empty")) }
                items(files, key = { it.key }) { file ->
                    val media = remember(file) { file.media() }
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(media.fileTileTitle ?: media.title, style = MaterialTheme.typography.titleMedium)
                            Text(media.fileName, style = MaterialTheme.typography.bodyMedium)
                            val tags = listOfNotNull(media.parsedName.episodeCode) + media.qualityTags
                            if (tags.isNotEmpty()) Text(tags.joinToString(" | "))
                            Button(onClick = {
                                launchError = false
                                try { context.startActivity(PlayerActivity.cloudIntent(context, file)) }
                                catch (_: Exception) { launchError = true }
                            }, enabled = file.ready && !state.busy) { Text(text(if (file.ready) "play" else "pending")) }
                        }
                    }
                }
                if (state.nextOffset != null) item {
                    Button(onClick = { model.refresh(more = true) }, enabled = !state.busy) { Text(text("load_more")) }
                }
                if (state.files.size >= TorBoxViewModel.MAX_FILES) item { Text(text("limit")) }
            }
        }
    }
    if (disconnecting) AlertDialog(
        onDismissRequest = { disconnecting = false },
        title = { Text(text("disconnect")) }, text = { Text(text("disconnect_detail")) },
        confirmButton = { TextButton(onClick = { disconnecting = false; draft = ""; model.disconnect() }) { Text(text("disconnect")) } },
        dismissButton = { TextButton(onClick = { disconnecting = false }) { Text(s.commonCancel) } },
    )
}

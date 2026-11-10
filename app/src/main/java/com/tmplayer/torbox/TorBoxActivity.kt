package com.tmplayer.torbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmplayer.data.FormFactor
import com.tmplayer.data.guardedLifecycleScope as lifecycleScope
import com.tmplayer.ui.i18n.LocalStrings
import com.tmplayer.ui.settings.SendLogsButton
import com.tmplayer.ui.theme.TMPlayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** TorBox account settings. Media discovery and playback are part of the shared Home experience. */
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
internal fun TorBoxScreen(onBack: () -> Unit, model: TorBoxViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val tv = FormFactor.isTv(context)
    val first = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var focusedInitially by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var disconnecting by remember { mutableStateOf(false) }
    fun text(key: String) = strings.text("torbox.$key")

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
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (tv) 48.dp else 20.dp),
            contentPadding = PaddingValues(vertical = if (tv) 27.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    com.tmplayer.ui.components.BrandLockup(
                        size = com.tmplayer.ui.components.BrandSize.Small,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onBack, modifier = Modifier.focusRequester(first)) {
                        Text(strings.commonBack)
                    }
                }
                Text(text("title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Connect your TorBox account here. Your Torrents, Usenet and Web downloads appear in Home alongside Telegram.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.busy) item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = model::cancel) { Text(strings.commonCancel) }
            }
            state.error?.let { reason ->
                item {
                    Card {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text("error_$reason"), color = MaterialTheme.colorScheme.error)
                            SendLogsButton()
                        }
                    }
                }
            }
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text(if (state.connected) "connected" else "disconnected"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        state.account?.let { account ->
                            if (account.email.isNotBlank()) Text(account.email)
                            if (account.plan.isNotBlank()) Text(text("plan") + ": " + account.plan)
                            if (account.expires.isNotBlank()) Text(text("expires") + ": " + account.expires)
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 512) draft = it },
                            label = { Text(text("api_key")) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false,
                            ),
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { model.connect(draft) { draft = "" } },
                            enabled = !state.busy && draft.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text(if (state.connected) "replace_key" else "connect"))
                        }
                        if (state.connected) {
                            OutlinedButton(
                                onClick = model::refresh,
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(text("refresh")) }
                            OutlinedButton(
                                onClick = { disconnecting = true },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(text("disconnect")) }
                        } else if (state.error == "storage") {
                            OutlinedButton(
                                onClick = { disconnecting = true },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(text("disconnect")) }
                        }
                    }
                }
            }
        }
    }

    if (disconnecting) AlertDialog(
        onDismissRequest = { disconnecting = false },
        title = { Text(text("disconnect")) },
        text = { Text(text("disconnect_detail")) },
        confirmButton = {
            TextButton(onClick = {
                disconnecting = false
                draft = ""
                model.disconnect()
            }) { Text(text("disconnect")) }
        },
        dismissButton = {
            TextButton(onClick = { disconnecting = false }) { Text(strings.commonCancel) }
        },
    )
}

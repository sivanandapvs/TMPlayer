package com.tmplayer.torbox

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.tmplayer.player.PlayerActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = Application::class)
class TorBoxPlaybackIntentTest {
    @Test
    @Config(sdk = [28], qualifiers = "w960dp-h540dp-land-television-night-xhdpi")
    fun missingCredentialsOfferRetryBackAndLogsWithoutTelegramSignIn() {
        com.tmplayer.data.FormFactor.override(true)
        val app = ApplicationProvider.getApplicationContext<Application>()
        java.io.File(app.noBackupFilesDir, "torbox-credential").delete()
        val intent = PlayerActivity.cloudIntent(app, TorBoxFile(42, 0, "Example.mp4", 5000, "", true))
        val controller = Robolectric.buildActivity(PlayerActivity::class.java, intent).setup()
        try {
            val activity = controller.get()
            val retry = activity.findViewById<View>(com.tmplayer.R.id.status_retry)
            val until = System.nanoTime() + 10_000_000_000L
            while (retry.visibility != View.VISIBLE && System.nanoTime() < until) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertFalse(activity.isFinishing)
            assertEquals(View.VISIBLE, retry.visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(com.tmplayer.R.id.status_back).visibility)
            val logs = org.robolectric.util.ReflectionHelpers.getField<View?>(activity, "statusLogs")
            assertNotNull(logs)
            assertEquals(View.VISIBLE, logs?.visibility)
        } finally {
            controller.pause().stop().destroy()
            com.tmplayer.data.FormFactor.override(false)
        }
    }

    @Test fun cloudPlaybackUsesTheExistingPlayerWithoutPersistingSignedLinks() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val file = TorBoxFile(42, 0, "library/Example.S01E02.1080p.mkv", 5000, "", true)
        val intent = PlayerActivity.cloudIntent(app, file)
        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals(42L, intent.getLongExtra("torbox_torrent_id", -1))
        assertEquals(0L, intent.getLongExtra("torbox_file_id", -1))
        assertEquals("Example.S01E02.1080p.mkv", intent.getStringExtra("file_name"))
        assertFalse(intent.getBooleanExtra("can_be_saved", true))
        assertNull(intent.data)
        assertFalse(intent.extras?.keySet().orEmpty().any { it.contains("token") || it.contains("url") || it.contains("api_key") })
    }

    @Test fun anAbsentCredentialDoesNotCreateAPlaintextPreferenceOrKeystoreEntry() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertNull(TorBoxCredentials(app).read())
        assertFalse(java.io.File(app.noBackupFilesDir, "torbox-credential").exists())
    }

    @Test fun aTruncatedCredentialFailsClosed() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val file = java.io.File(app.noBackupFilesDir, "torbox-credential")
        file.writeBytes(byteArrayOf(1, 2, 3))
        try {
            TorBoxCredentials(app).read()
            fail("Truncated ciphertext must not become an API key")
        } catch (failure: TorBoxFailure) {
            assertEquals("storage", failure.reason)
        } finally {
            file.delete()
        }
    }
}

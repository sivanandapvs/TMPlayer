package com.tmplayer.ui.browse

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.tmplayer.ui.online.TitleArt
import com.tmplayer.ui.theme.TMPlayerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TitleArtImmediateTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun `local title is visible on first frame while metadata is pending`() {
        rule.setContent {
            TMPlayerTheme {
                TitleArt(meta = null, maxWidth = 280.dp, maxHeight = 90.dp, settled = false) {
                    Text("Night Train")
                }
            }
        }
        rule.onNodeWithText("Night Train").assertExists()
    }

    @Test fun `changing carousel item never retains the previous title`() {
        var title by mutableStateOf("Night Train")
        rule.setContent {
            TMPlayerTheme {
                TitleArt(meta = null, maxWidth = 280.dp, maxHeight = 90.dp, settled = false) {
                    Text(title)
                }
            }
        }
        rule.onNodeWithText("Night Train").assertExists()
        rule.runOnIdle { title = "Paper Moons" }
        rule.onNodeWithText("Paper Moons").assertExists()
    }
}

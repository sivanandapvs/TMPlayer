package com.tmplayer.torbox

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.tmplayer.data.FormFactor
import com.tmplayer.i18n.L
import com.tmplayer.ui.theme.TMPlayerTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w960dp-h540dp-land-television-night-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TorBoxFocusTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun television() { FormFactor.override(true) }
    @After fun reset() { FormFactor.override(false) }

    @Test fun backHasInitialFocusAndDownReachesTheCredentialField() {
        val viewRoot = rule.activity.window.decorView.parent
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<Any>(
            viewRoot, "ensureTouchMode",
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, false),
        )
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<Any>(
            viewRoot, "windowFocusChanged",
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true),
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, false),
        )
        rule.setContent { TMPlayerTheme { TorBoxScreen(onBack = {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithText(L.commonCancel).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText(L.commonBack).assertIsFocused()
        rule.onNodeWithText(L.commonBack).performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNode(hasSetTextAction()).assertIsFocused()
    }
}

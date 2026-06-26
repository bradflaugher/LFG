package com.bradflaugher.lfg

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LaunchTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test fun appBootsToAgentScreen() {
    // Splash flips to content after ~1s; the chat screen renders the Settings (gear) icon
    // in its top app bar, so we wait for that as a reliable signal the agent UI is up.
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
        .onAllNodesWithContentDescription("Settings", substring = true, ignoreCase = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
  }

  @Test fun applicationPackageIsLfg() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.bradflaugher.lfg", ctx.packageName)
  }
}

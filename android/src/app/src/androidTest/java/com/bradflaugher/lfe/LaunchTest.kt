/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Smoke tests that exercise the full Hilt graph + Compose nav. If MainActivity
 * crashes, these fail. If the agent screen doesn't render, these fail.
 *
 * No `HiltAndroidRule` here on purpose: `LfeApplication` is `@HiltAndroidApp`
 * and initializes naturally when the test process starts the activity, so
 * production DI is exercised end-to-end without a separate test entry point.
 */
package com.bradflaugher.lfe

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
    // Splash flips to content after ~1s; wait until any expected element appears.
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
        .onAllNodesWithText("Agent Skills", substring = true, ignoreCase = true)
        .fetchSemanticsNodes()
        .isNotEmpty() ||
        composeRule
          .onAllNodesWithText("Introducing", substring = true, ignoreCase = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test fun applicationPackageIsLfe() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.bradflaugher.lfe", ctx.packageName)
  }
}

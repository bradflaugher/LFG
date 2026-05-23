/*
 * LFE — Apache 2.0.
 *
 * Smoke test: the app launches directly to the agent skills chat (no home, no carousel).
 *
 * NB: Requires an Android device/emulator. CI doesn't run this — it's a local sanity check.
 * The CI pipeline only runs the JVM-side `test` task.
 */

package com.bradflaugher.lfe

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {
  @Test fun appLaunchesToMainActivity() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertNotNull("activity started", activity)
      }
    }
  }

  @Test fun applicationPackageIsLfe() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    assert(ctx.packageName == "com.bradflaugher.lfe") {
      "Expected com.bradflaugher.lfe, got ${ctx.packageName}"
    }
  }
}

package com.bradflaugher.lfg

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.bradflaugher.lfg.ui.theme.LfgTheme
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val modelManagerViewModel: ModelManagerViewModel by viewModels()
  private var splashScreenAboutToExit: Boolean = false
  private var contentSet: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    // Pass null to discard saved instance state so the app starts cleanly on the agent
    // screen after an OS kill instead of restoring an inconsistent nav stack.
    super.onCreate(null)

    fun setContent() {
      if (contentSet) return

      setContent {
        LfgTheme {
          Surface(modifier = Modifier.fillMaxSize()) {
            LfgApp(modelManagerViewModel = modelManagerViewModel)

            var startMaskFadeout by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { startMaskFadeout = true }
            AnimatedVisibility(
              !startMaskFadeout,
              enter = fadeIn(animationSpec = snap(0)),
              exit =
                fadeOut(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
            ) {
              Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
              )
            }
          }
        }
      }

      @OptIn(ExperimentalApi::class)
      ExperimentalFlags.enableBenchmark = false

      contentSet = true
    }

    modelManagerViewModel.loadModelAllowlist()

    val splashScreen = installSplashScreen()

    lifecycleScope.launch {
      delay(1000)
      if (!splashScreenAboutToExit) setContent()
    }

    splashScreen.setOnExitAnimationListener { splashScreenView ->
      splashScreenAboutToExit = true

      val now = System.currentTimeMillis()
      val iconAnimationStartMs = splashScreenView.iconAnimationStartMillis
      val duration = splashScreenView.iconAnimationDurationMillis
      val fadeOut = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
      fadeOut.interpolator = DecelerateInterpolator()
      fadeOut.duration = 300L
      fadeOut.doOnEnd { splashScreenView.remove() }
      lifecycleScope.launch {
        val setContentDelay = duration - (now - iconAnimationStartMs) - 300
        if (setContentDelay > 0) delay(setContentDelay)
        setContent()
        fadeOut.start()
      }
    }

    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // Fix for three-button nav not properly going edge-to-edge:
      // https://issuetracker.google.com/issues/298296168
      window.isNavigationBarContrastEnforced = false
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }
}

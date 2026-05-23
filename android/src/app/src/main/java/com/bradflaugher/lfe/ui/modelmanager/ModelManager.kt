/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfe.ui.modelmanager

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfe.ui.preview.PreviewModelManagerViewModel
// import com.bradflaugher.lfe.ui.preview.TASK_TEST1
// import com.bradflaugher.lfe.ui.theme.LfeTheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bradflaugher.lfe.GalleryTopAppBar
import com.bradflaugher.lfe.data.AppBarAction
import com.bradflaugher.lfe.data.AppBarActionType
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.Task

/** A screen to manage models. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManager(
  task: Task,
  viewModel: ModelManagerViewModel,
  enableAnimation: Boolean,
  navigateUp: () -> Unit,
  onModelClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  onBenchmarkClicked: (Model) -> Unit = {},
  onBrowseHuggingFace: (() -> Unit)? = null,
) {
  // Set title based on the task.
  val title = task.label
  // Model count.
  val modelCount by remember {
    derivedStateOf {
      val trigger = task.updateTrigger.value
      if (trigger >= 0) {
        task.models.size
      } else {
        -1
      }
    }
  }

  // Navigate up when there are no models left.
  LaunchedEffect(modelCount) {
    if (modelCount == 0) {
      navigateUp()
    }
  }

  // Handle system's edge swipe.
  BackHandler { navigateUp() }

  Scaffold(
    modifier = modifier,
    topBar = {
      GalleryTopAppBar(
        title = title,
        leftAction = AppBarAction(actionType = AppBarActionType.NAVIGATE_UP, actionFn = navigateUp),
      )
    },
    floatingActionButton = {
      if (onBrowseHuggingFace != null) {
        ExtendedFloatingActionButton(
          onClick = onBrowseHuggingFace,
          icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          text = { Text("Browse HF") },
        )
      }
    },
  ) { innerPadding ->
    ModelList(
      task = task,
      modelManagerViewModel = viewModel,
      contentPadding = innerPadding,
      enableAnimation = enableAnimation,
      onModelClicked = onModelClicked,
      onBenchmarkClicked = onBenchmarkClicked,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.ui.modelmanager

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bradflaugher.lfg.GalleryTopAppBar
import com.bradflaugher.lfg.data.AppBarAction
import com.bradflaugher.lfg.data.AppBarActionType
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.Task

/** A screen to manage models. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManager(
  task: Task,
  viewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onModelClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  onBenchmarkClicked: (Model) -> Unit = {},
) {
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

  // State for the local-file model import flow.
  var pickedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }
  var importingInfo by remember { mutableStateOf<com.bradflaugher.lfg.proto.ImportedModel?>(null) }
  var showImportingDialog by remember { mutableStateOf(false) }
  var showCloudProviderDialog by remember { mutableStateOf(false) }

  val filePickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          pickedFileUri = uri
          showImportDialog = true
        }
      }
    }

  val launchPicker = {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
      }
    filePickerLauncher.launch(intent)
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      GalleryTopAppBar(
        title = "Models",
        leftAction = AppBarAction(actionType = AppBarActionType.NAVIGATE_UP, actionFn = navigateUp),
      )
    },
  ) { innerPadding ->
    ModelList(
      task = task,
      modelManagerViewModel = viewModel,
      contentPadding = innerPadding,
      onModelClicked = onModelClicked,
      onBenchmarkClicked = onBenchmarkClicked,
      onImportLocalModelClicked = launchPicker,
      modifier = Modifier.fillMaxSize(),
    )
  }

  if (showCloudProviderDialog) {
    CloudProviderDialog(
      dataStoreRepository = viewModel.dataStoreRepository,
      onDismiss = { showCloudProviderDialog = false },
      onSettingsSaved = {
        viewModel.refreshCloudModelDisplayName()
      }
    )
  }

  if (showImportDialog) {
    pickedFileUri?.let { uri ->
      ModelImportDialog(
        uri = uri,
        onDismiss = { showImportDialog = false },
        onDone = { info ->
          importingInfo = info
          showImportDialog = false
          showImportingDialog = true
        },
      )
    }
  }

  if (showImportingDialog) {
    pickedFileUri?.let { uri ->
      importingInfo?.let { info ->
        ModelImportingDialog(
          uri = uri,
          info = info,
          onDismiss = { showImportingDialog = false },
          onDone = {
            viewModel.addImportedLlmModel(info = it)
            showImportingDialog = false
          },
        )
      }
    }
  }
}

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
package com.bradflaugher.lfe.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.data.ConfigKeys
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.ModelCapability
import com.bradflaugher.lfe.data.ModelDownloadStatusType
import com.bradflaugher.lfe.data.Task
import com.bradflaugher.lfe.data.convertValueToTargetType
import com.bradflaugher.lfe.ui.modelmanager.ModelInitializationStatusType
import com.bradflaugher.lfe.ui.modelmanager.ModelManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPageAppBar(
  task: Task,
  model: Model,
  modelManagerViewModel: ModelManagerViewModel,
  onBackClicked: () -> Unit,
  onModelSelected: (prev: Model, cur: Model) -> Unit,
  inProgress: Boolean,
  modelPreparing: Boolean,
  modifier: Modifier = Modifier,
  hideModelSelector: Boolean = false,
  onConfigChanged: (oldConfigValues: Map<String, Any>, newConfigValues: Map<String, Any>) -> Unit =
    { _, _ ->
    },
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  shouldShowHistoryButton: Boolean = false,
  onWebLoginClicked: () -> Unit = {},
  onHistoryClicked: (Model) -> Unit = {},
  onSettingsClicked: () -> Unit = {},
  hideBackButton: Boolean = false,
) {
  var showConfigDialog by remember { mutableStateOf(false) }
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[model.name]
  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[model.name]
  val isModelInitializing =
    modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZING
  val isModelInitialized =
    modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZED

  CenterAlignedTopAppBar(
    title = {
      // Model chips pager — the task name/icon was removed (it just said "Agent Skills"
      // for the single chat task and was leftover gallery chrome).
      if (!hideModelSelector) {
        val enableModelPickerChip = !isModelInitializing && !inProgress
        ModelPickerChip(
          enabled = enableModelPickerChip,
          task = task,
          initialModel = model,
          modelManagerViewModel = modelManagerViewModel,
          onModelSelected = onModelSelected,
        )
      }
    },
    modifier = modifier,
    // Left side: back arrow on subscreens, gear (settings) on the chat home screen.
    // Putting the gear here keeps it from overlapping the model-config and history
    // buttons on the right once a chat has started.
    navigationIcon = {
      if (!hideBackButton) {
        val enableBackButton = !isModelInitializing && !inProgress
        IconButton(onClick = onBackClicked, enabled = enableBackButton) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_navigate_back_icon),
          )
        }
      } else {
        IconButton(onClick = onSettingsClicked) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.cd_app_settings_icon),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    },
    // The config button for the model (if existed).
    actions = {
      val downloadSucceeded = curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
      val showConfigButton = model.configs.isNotEmpty() && downloadSucceeded
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (downloadSucceeded) {
          val enableWebLoginButton = !isModelInitializing && !inProgress
          IconButton(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              onWebLoginClicked()
            },
            enabled = enableWebLoginButton,
            modifier = Modifier.alpha(if (!enableWebLoginButton) 0.5f else 1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.Language,
              contentDescription = "Web Browser",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (showConfigButton) {
          val enableConfigButton = !isModelInitializing && !inProgress && isModelInitialized
          IconButton(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              showConfigDialog = true
            },
            enabled = enableConfigButton,
            modifier = Modifier.alpha(if (!enableConfigButton) 0.5f else 1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = stringResource(R.string.cd_model_settings_icon),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (downloadSucceeded && shouldShowHistoryButton) {
          val enableHistoryButton =
            !isModelInitializing && !modelPreparing && !inProgress && isModelInitialized
          IconButton(
            onClick = { onHistoryClicked(model) },
            enabled = enableHistoryButton,
            modifier = Modifier.alpha(if (!enableHistoryButton) 0.5f else 1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = stringResource(R.string.cd_chat_history),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    },
  )

  // Config dialog.
  if (showConfigDialog) {
    val modelConfigs = model.configs.toMutableList()
    modelConfigs.removeIf { it.key == ConfigKeys.RESET_CONVERSATION_TURN_COUNT }
    if (!task.allowCapability(ModelCapability.LLM_THINKING, model)) {
      modelConfigs.removeIf { it.key == ConfigKeys.ENABLE_THINKING }
    }
    var supportsSpeculativeDecoding = false
    // Check if the model file supports speculative decoding.
    try {
      com.google.ai.edge.litertlm.Capabilities(model.getPath(context)).use {
        supportsSpeculativeDecoding = it.hasSpeculativeDecodingSupport()
      }
    } catch (e: Exception) {
      // Ignore exceptions and assume not supported.
    }
    if (
      !supportsSpeculativeDecoding ||
      !task.allowCapability(ModelCapability.SPECULATIVE_DECODING, model)
    ) {
      modelConfigs.removeIf { it.key == ConfigKeys.ENABLE_SPECULATIVE_DECODING }
    }
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      modelConfigs.removeIf { it.key == ConfigKeys.ACCELERATOR }
    }
    ConfigDialog(
      title = "Configurations",
      configs = modelConfigs,
      initialValues = model.configValues,
      onDismissed = { showConfigDialog = false },
      isCloudModel = model.name == "Cloud-Model-OpenAI-Compatible",
      dataStoreRepository = modelManagerViewModel.dataStoreRepository,
      onOk = { curConfigValues, oldSystemPrompt, newSystemPrompt ->
        // Hide config dialog.
        showConfigDialog = false

        // Check if the configs are changed or not. Also check if the model needs to be
        // re-initialized.
        var same = true
        var needReinitialization = false
        for (config in modelConfigs) {
          val key = config.key.label
          val oldValue =
            convertValueToTargetType(
              value = model.configValues.getValue(key),
              valueType = config.valueType,
            )
          val newValue =
            convertValueToTargetType(
              value = curConfigValues.getValue(key),
              valueType = config.valueType,
            )
          if (oldValue != newValue) {
            same = false
            if (config.needReinitialization) {
              needReinitialization = true
            }
            break
          }
        }
        if (same) {
          if (newSystemPrompt != oldSystemPrompt) {
            onSystemPromptChanged(newSystemPrompt)
          }
          return@ConfigDialog
        }

        // Save the config values to Model.
        val oldConfigValues = model.configValues
        model.prevConfigValues = oldConfigValues
        model.configValues = curConfigValues
        modelManagerViewModel.updateConfigValuesUpdateTrigger()

        if (!task.handleModelConfigChangesInTask) {
          // Force to re-initialize the model with the new configs.
          if (needReinitialization) {
            modelManagerViewModel.initializeModel(
              context = context,
              task = task,
              model = model,
              force = true,
              onDone = {
                if (oldSystemPrompt != newSystemPrompt) {
                  onSystemPromptChanged(newSystemPrompt)
                }
              },
            )
          }

          // Notify.
          onConfigChanged(oldConfigValues, model.configValues)
        }
      },
      showSystemPromptEditorTab = allowEditingSystemPrompt,
      defaultSystemPrompt = task.defaultSystemPrompt,
      curSystemPrompt = curSystemPrompt,
    )
  }
}

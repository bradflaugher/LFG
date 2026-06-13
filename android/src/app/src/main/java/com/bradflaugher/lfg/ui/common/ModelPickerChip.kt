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
package com.bradflaugher.lfg.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfg.R
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.modelitem.StatusIcon
import com.bradflaugher.lfg.ui.modelmanager.ModelInitializationStatusType
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerChip(
  enabled: Boolean,
  task: Task,
  initialModel: Model,
  modelManagerViewModel: ModelManagerViewModel,
  onModelSelected: (prev: Model, cur: Model) -> Unit,
) {
  var showModelPicker by remember { mutableStateOf(false) }
  var modelPickerModel by remember { mutableStateOf<Model?>(null) }
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val density = LocalDensity.current
  val windowInfo = LocalWindowInfo.current
  val screenWidthDp = remember { with(density) { windowInfo.containerSize.width.toDp() } }

  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[initialModel.name]

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      val modelName = initialModel.displayName.ifEmpty { initialModel.name }
      val cdChangeModel = stringResource(R.string.cd_change_model, modelName)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
          Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled) {
              modelPickerModel = initialModel
              showModelPicker = true
            }
            .padding(start = 8.dp, end = 2.dp)
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.6f }
            .semantics { contentDescription = cdChangeModel },
      ) {
        if (modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZING) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(21.dp)) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp).alpha(0.5f),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Text(
          modelName,
          style = MaterialTheme.typography.labelLarge,
          modifier =
            Modifier.padding(start = 4.dp)
              .widthIn(0.dp, screenWidthDp - 250.dp)
              .clearAndSetSemantics {},
          maxLines = 1,
          overflow = TextOverflow.MiddleEllipsis,
        )
        Icon(
          Icons.Rounded.ArrowDropDown,
          modifier = Modifier.size(20.dp),
          contentDescription = null, // handled by Row semantics
        )
      }
    }
  }

  // Model picker.
  val curModelPickerModel = modelPickerModel
  if (showModelPicker && curModelPickerModel != null) {
    ModalBottomSheet(onDismissRequest = { showModelPicker = false }, sheetState = sheetState) {
      ModelPicker(
        task = task,
        modelManagerViewModel = modelManagerViewModel,
        onModelSelected = { selectedModel ->
          showModelPicker = false
          val prevSelectedModel = modelManagerUiState.selectedModel
          onModelSelected(prevSelectedModel, selectedModel)
        },
      )
    }
  }
}

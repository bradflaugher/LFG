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

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfg.ui.preview.PreviewModelManagerViewModel
// import com.bradflaugher.lfg.ui.preview.TASK_TEST1
// import com.bradflaugher.lfg.ui.theme.LfgTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradflaugher.lfg.R
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.modelitem.StatusIcon
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.bradflaugher.lfg.ui.theme.labelSmallNarrow

@Composable
fun ModelPicker(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  onModelSelected: (Model) -> Unit,
) {
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  var showMemoryWarning by remember { mutableStateOf(false) }
  var modelToPick by remember { mutableStateOf<Model?>(null) }
  val context = LocalContext.current

  Column(modifier = Modifier.padding(bottom = 8.dp)) {
    Text(
      "Models",
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )

    // Model list.
    for (model in task.models) {
      val selected = model.name == modelManagerUiState.selectedModel.name
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
          Modifier.fillMaxWidth()
            .clickable {
              // Show memory warning before proceeding.
              if (isMemoryLow(context = context, model = model)) {
                modelToPick = model
                showMemoryWarning = true
              } else {
                onModelSelected(model)
              }
            }
            .background(
              if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            model.displayName.ifEmpty { model.name },
            style = MaterialTheme.typography.bodyMedium,
          )
          run {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              StatusIcon(
                task = task,
                model = model,
                downloadStatus = modelManagerUiState.modelDownloadStatus[model.name],
              )
              if (model.name != "Cloud-Model-OpenAI-Compatible") {
                Text(
                  if (model.localFileRelativeDirPathOverride.isEmpty()) {
                    model.sizeInBytes.humanReadableSize()
                  } else {
                    "{ext_file_dir}/${model.localFileRelativeDirPathOverride}"
                  },
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = labelSmallNarrow.copy(lineHeight = 10.sp),
                )
              }
            }
          }
        }
        if (selected) {
          Icon(
            Icons.Filled.CheckCircle,
            modifier = Modifier.size(16.dp),
            contentDescription = stringResource(R.string.cd_selected_icon),
          )
        }
      }
    }
  }

  if (showMemoryWarning) {
    MemoryWarningAlert(
      onProceeded = {
        val curModelToPick = modelToPick
        if (curModelToPick != null) {
          onModelSelected(curModelToPick)
        }
        showMemoryWarning = false
      },
      onDismissed = { showMemoryWarning = false },
    )
  }
}

// @Preview(showBackground = true)
// @Composable
// fun ModelPickerPreview() {
//   val context = LocalContext.current

//   LfgTheme {
//     ModelPicker(
//       task = TASK_TEST1,
//       modelManagerViewModel = PreviewModelManagerViewModel(context = context),
//       onModelSelected = {},
//     )
//   }
// }

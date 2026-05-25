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

import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.Task
import com.bradflaugher.lfe.ui.common.ClickableLink
import com.bradflaugher.lfe.ui.common.getTaskBgColor
import com.bradflaugher.lfe.ui.common.modelitem.ModelItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

private const val TAG = "AGModelList"

/** The list of models in the model manager. */
@Composable
fun ModelList(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  contentPadding: PaddingValues,
  onModelClicked: (Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  onImportLocalModelClicked: (() -> Unit)? = null,
) {
  // This is just to update "models" list when task.updateTrigger is updated so that the UI can
  // be properly updated.
  val models by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { !it.imported }
        } else {
          listOf()
        }
      }
    }
  val importedModels by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { it.imported }
        } else {
          listOf()
        }
      }
    }
  val modelVariants by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models
            .toList()
            .filter { it.parentModelName != null }
            .groupBy { it.parentModelName!! }
        } else {
          mapOf()
        }
      }
    }

  val listState = rememberLazyListState()
  val modelItemExpandedStates = remember { mutableStateMapOf<String, Boolean>() }

  Box(
    contentAlignment = Alignment.BottomEnd,
    modifier = Modifier.background(color = getTaskBgColor(task = task)),
  ) {
    LazyColumn(
      modifier = modifier.padding(horizontal = 16.dp),
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      state = listState,
    ) {
      // Header area. The task label ("Agent Skills") and its icon used to live here
      // — both were Gallery-app leftovers and are intentionally gone. We keep just
      // the model-count caption + any task-level reference links the task carries.
      item(key = "taskHeader") {
        Spacer(modifier = Modifier.height(8.dp))
      }

      // List of models within a task.
      items(items = models) { model ->
        if (model.parentModelName.isNullOrEmpty()) {
          ModelItem(
            model = model,
            modelVariants = modelVariants.getOrDefault(model.name, listOf()),
            task = task,
            modelManagerViewModel = modelManagerViewModel,
            onModelClicked = onModelClicked,
            onBenchmarkClicked = onBenchmarkClicked,
            expanded = true,
            onExpanded = {},
            showBenchmarkButton = true,
          )
        }
      }

      // Title for imported models.
      if (importedModels.isNotEmpty()) {
        item(key = "importedModelsTitle") {
          Text(
            stringResource(R.string.model_list_imported_models_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
          )
        }
      }

      // List of imported models within a task.
      items(items = importedModels, key = { it.name }) { model ->
        Box {
          ModelItem(
            model = model,
            task = task,
            modelManagerViewModel = modelManagerViewModel,
            onModelClicked = onModelClicked,
            onBenchmarkClicked = onBenchmarkClicked,
            showBenchmarkButton = true,
          )
        }
      }

      // Actions: lives at the bottom of the list so it doesn't compete with model selection above.
      if (onImportLocalModelClicked != null) {
        item(key = "modelActions") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(onClick = onImportLocalModelClicked) {
              Icon(
                Icons.AutoMirrored.Outlined.NoteAdd,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
              )
              Text("Import local model file")
            }
          }
        }
      }

    }

    // Gradient overlay at the bottom.
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(contentPadding.calculateBottomPadding())
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainer),
            ),
          )
          .align(Alignment.BottomCenter),
    )
  }
}

// @Preview(showBackground = true)
// @Composable
// fun ModelListPreview() {
//   val context = LocalContext.current

//   LfeTheme {
//     ModelList(
//       task = TASK_TEST1,
//       modelManagerViewModel = PreviewModelManagerViewModel(context = context),
//       onModelClicked = {},
//       contentPadding = PaddingValues(all = 16.dp),
//     )
//   }
// }

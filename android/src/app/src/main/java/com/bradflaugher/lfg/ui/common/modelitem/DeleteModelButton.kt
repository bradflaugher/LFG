/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.ui.common.modelitem

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.bradflaugher.lfg.R
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.ModelDownloadStatus
import com.bradflaugher.lfg.data.ModelDownloadStatusType
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel

/** Composable function to display a button for deleting the downloaded model. */
@Composable
fun DeleteModelButton(
  model: Model,
  modelManagerViewModel: ModelManagerViewModel,
  downloadStatus: ModelDownloadStatus?,
  modifier: Modifier = Modifier,
  showDeleteButton: Boolean = true,
) {
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    when (downloadStatus?.status) {
      // Button to delete the download.
      ModelDownloadStatusType.SUCCEEDED -> {
        if (showDeleteButton) {
          IconButton(onClick = { showConfirmDeleteDialog = true }) {
            Icon(
              Icons.Outlined.Delete,
              contentDescription = stringResource(R.string.cd_delete_icon),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.alpha(0.6f),
            )
          }
        }
      }

      else -> {}
    }
  }

  if (showConfirmDeleteDialog) {
    ConfirmDeleteModelDialog(
      model = model,
      onConfirm = {
        modelManagerViewModel.deleteModel(model = model)
        showConfirmDeleteDialog = false
      },
      onDismiss = { showConfirmDeleteDialog = false },
    )
  }
}

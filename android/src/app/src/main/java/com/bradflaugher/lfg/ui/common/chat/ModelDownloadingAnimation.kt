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
package com.bradflaugher.lfg.ui.common.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.ModelDownloadStatusType
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.RotationalLoader
import com.bradflaugher.lfg.ui.common.formatToHourMinSecond
import com.bradflaugher.lfg.ui.common.humanReadableSize
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.bradflaugher.lfg.ui.theme.labelSmallNarrow

/**
 * Composable function to display a loading animation using a 2x2 grid of images with a synchronized
 * scaling and rotation effect.
 */
@Composable
fun ModelDownloadingAnimation(
  model: Model,
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
) {
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val downloadStatus by remember {
    derivedStateOf { modelManagerUiState.modelDownloadStatus[model.name] }
  }
  val inProgress = downloadStatus?.status == ModelDownloadStatusType.IN_PROGRESS
  val isPartiallyDownloaded = downloadStatus?.status == ModelDownloadStatusType.PARTIALLY_DOWNLOADED
  var curDownloadProgress = 0f

  // Failure message.
  val curDownloadStatus = downloadStatus
  if (curDownloadStatus != null && curDownloadStatus.status == ModelDownloadStatusType.FAILED) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        curDownloadStatus.errorMessage,
        color = MaterialTheme.colorScheme.error,
        style = labelSmallNarrow,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
  // No failure
  else {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 32.dp),
    ) {
      // Loader.
      RotationalLoader(size = 160.dp)

      Spacer(modifier = Modifier.height(32.dp))

      // Download stats
      var sizeLabel = model.totalBytes.humanReadableSize()
      if (curDownloadStatus != null) {
        // For in-progress model, show {receivedSize} / {totalSize} - {rate} - {remainingTime}
        if (inProgress || isPartiallyDownloaded) {
          var totalSize = curDownloadStatus.totalBytes
          if (totalSize == 0L) {
            totalSize = model.totalBytes
          }
          sizeLabel =
            "${curDownloadStatus.receivedBytes.humanReadableSize(extraDecimalForGbAndAbove = true)} of ${totalSize.humanReadableSize()}"
          if (curDownloadStatus.bytesPerSecond > 0) {
            sizeLabel = "$sizeLabel · ${curDownloadStatus.bytesPerSecond.humanReadableSize()} / s"
            if (curDownloadStatus.remainingMs >= 0) {
              sizeLabel =
                "$sizeLabel · ${curDownloadStatus.remainingMs.formatToHourMinSecond()} left"
            }
          }
          if (isPartiallyDownloaded) {
            sizeLabel = "$sizeLabel (resuming...)"
          }
          curDownloadProgress =
            curDownloadStatus.receivedBytes.toFloat() / curDownloadStatus.totalBytes.toFloat()
          if (curDownloadProgress.isNaN()) {
            curDownloadProgress = 0f
          }
        }
        // Status for unzipping.
        else if (curDownloadStatus.status == ModelDownloadStatusType.UNZIPPING) {
          sizeLabel = "Unzipping..."
        }
        Text(
          sizeLabel,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium,
          textAlign = TextAlign.Center,
          overflow = TextOverflow.Visible,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }
    }
  }
}

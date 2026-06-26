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
package com.bradflaugher.lfg.ui.common.chat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfg.R

/**
 * A container that wraps a composable with long-press gesture detection to show a dropdown menu for
 * copying text.
 */
@Composable
fun LongPressCopyContainer(
  copyText: String,
  modifier: Modifier = Modifier,
  onCopyClicked: (String) -> Unit = {},
  content: @Composable () -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }
  val haptic = LocalHapticFeedback.current
  val moreOptionsLabel = stringResource(R.string.cd_more_options)
  Box(
    modifier =
      modifier
        .pointerInput(Unit) {
          detectTapGestures(
            onLongPress = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showMenu = true
            },
          )
        }
        .semantics {
          onLongClick(moreOptionsLabel) {
            showMenu = true
            true
          }
        },
  ) {
    content()
    DropdownMenu(
      expanded = showMenu,
      onDismissRequest = { showMenu = false },
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 8.dp,
      shadowElevation = 8.dp,
    ) {
      DropdownMenuItem(
        text = {
          Text(
            stringResource(R.string.copy),
            style =
              MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          )
        },
        leadingIcon = {
          Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = stringResource(R.string.copy),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = {
          showMenu = false
          onCopyClicked(copyText)
        },
      )
    }
  }
}

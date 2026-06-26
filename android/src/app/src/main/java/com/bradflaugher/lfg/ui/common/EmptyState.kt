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
package com.bradflaugher.lfg.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class EmptyStateButtonConfig(
  @StringRes val buttonLabelResId: Int,
  val buttonIcon: ImageVector? = null,
  val onButtonClick: () -> Unit = {},
  val extraContent: @Composable () -> Unit = {},
)

/**
 * A composable function to display an empty state with an icon, title, description, and an optional
 * button.
 */
@Composable
fun EmptyState(
  icon: ImageVector,
  @StringRes titleResId: Int,
  @StringRes descriptionResId: Int,
  buttonConfig: EmptyStateButtonConfig? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.padding(horizontal = 48.dp),
  ) {
    Icon(
      icon,
      contentDescription = null,
      modifier = Modifier.size(56.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      stringResource(titleResId),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Text(
      stringResource(descriptionResId),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (buttonConfig != null) {
      Box {
        Button(
          contentPadding = SMALL_BUTTON_CONTENT_PADDING,
          onClick = buttonConfig.onButtonClick,
        ) {
          if (buttonConfig.buttonIcon != null) {
            Icon(
              buttonConfig.buttonIcon,
              contentDescription = null,
              modifier = Modifier.padding(end = 8.dp).size(20.dp),
            )
          }
          Text(stringResource(buttonConfig.buttonLabelResId))
        }
        buttonConfig.extraContent()
      }
    }
  }
}

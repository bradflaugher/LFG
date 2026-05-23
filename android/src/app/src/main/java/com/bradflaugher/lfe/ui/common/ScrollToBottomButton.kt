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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bradflaugher.lfe.R

/**
 * A button that is animated to show or hide based on the [isAtBottom] state. When visible, clicking
 * it triggers the [onClick] action, typically used to scroll to the bottom of a view.
 */
@Composable
fun ScrollToBottomButton(
  isAtBottom: Boolean,
  onClick: () -> Unit,
) {
  AnimatedVisibility(
    visible = !isAtBottom,
    enter =
      fadeIn(animationSpec = tween(durationMillis = 300)) +
        scaleIn(
          animationSpec =
            spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMedium,
            ),
        ),
    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
  ) {
    IconButton(
      onClick = onClick,
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
      Icon(
        imageVector = Icons.Outlined.ArrowDownward,
        contentDescription = stringResource(R.string.cd_scroll_to_bottom),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}

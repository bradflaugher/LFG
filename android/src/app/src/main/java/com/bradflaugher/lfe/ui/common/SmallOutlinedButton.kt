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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BUTTON_CONTENT_PADDING =
  PaddingValues(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 2.dp)

/** A small OutlinedButton composable with a label and an optional icon. */
@Composable
fun SmallOutlinedButton(
  onClick: () -> Unit,
  labelResId: Int = 0,
  imageVector: ImageVector? = null,
  iconResId: Int? = null,
  size: Dp = 18.dp,
  label: String? = null,
  enabled: Boolean = true,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = Modifier.height(32.dp),
    contentPadding = BUTTON_CONTENT_PADDING,
    enabled = enabled,
  ) {
    if (imageVector != null) {
      Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(size))
    } else if (iconResId != null) {
      Icon(
        ImageVector.vectorResource(iconResId),
        contentDescription = null,
        modifier = Modifier.size(size),
      )
    }
    Text(
      text = label ?: if (labelResId != 0) stringResource(labelResId) else "",
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 4.dp),
    )
  }
}

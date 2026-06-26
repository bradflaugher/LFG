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

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfg.ui.theme.LfgTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfg.ui.theme.bodySmallNarrow

/** Composable function to display an action button below a chat message. */
@Composable
fun MessageActionButton(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val alpha: Float = if (enabled) 1.0f else 0.3f
  Row(
    modifier =
      modifier
        .padding(top = 4.dp)
        .clip(CircleShape)
        .background(
          if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          },
        )
        .clickable(enabled = enabled, onClickLabel = label) { onClick() },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      icon,
      contentDescription = null, // Handled by Row's click label
      modifier = Modifier.size(16.dp).offset(x = 6.dp).alpha(alpha),
    )
    Text(
      label,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      style = bodySmallNarrow,
      modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp).alpha(alpha),
    )
  }
}

// @Preview(showBackground = true)
// @Composable
// fun MessageActionButtonPreview() {
//   LfgTheme {
//     Column {
//       MessageActionButton(label = "run", icon = Icons.Default.PlayArrow, onClick = {})
//       MessageActionButton(
//         label = "run",
//         icon = Icons.Default.PlayArrow,
//         enabled = false,
//         onClick = {})
//     }
//   }
// }

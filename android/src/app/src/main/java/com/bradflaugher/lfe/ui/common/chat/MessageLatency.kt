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
package com.bradflaugher.lfe.ui.common.chat

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfe.ui.theme.LfeTheme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import com.bradflaugher.lfe.ui.common.humanReadableDuration

/** Composable function to display the latency of a chat message, if available. */
@Composable
fun LatencyText(message: ChatMessage) {
  if (message.latencyMs >= 0) {
    Text(
      message.latencyMs.humanReadableDuration(),
      modifier = Modifier.alpha(0.5f).testTag("latency_label"),
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

// @Preview(showBackground = true)
// @Composable
// fun LatencyTextPreview() {
//   LfeTheme {
//     Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp))
// {
//       for (latencyMs in listOf(123f, 1234f, 123456f, 7234567f)) {
//         LatencyText(
//           message =
//             ChatMessage(latencyMs = latencyMs, type = ChatMessageType.TEXT, side =
// ChatSide.AGENT)
//         )
//       }
//     }
//   }
// }

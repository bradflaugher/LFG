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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfe.R

data class MessageLayoutConfig(
  val horizontalArrangement: Arrangement.Horizontal,
  val modifier: Modifier,
  val userLabel: String,
  val rightSideLabel: String,
)

/**
 * Composable function to display the sender information for a chat message.
 *
 * This function handles different types of chat messages, including system messages, benchmark
 * results, and image generation results, and displays the appropriate sender label and status
 * information.
 */
@Composable
fun MessageSender(
  message: ChatMessage,
  agentName: String = "",
  imageHistoryCurIndex: Int = 0,
) {
  // No user label for system messages.
  if (message.side == ChatSide.SYSTEM) {
    return
  }

  val (horizontalArrangement, modifier, userLabel, rightSideLabel) =
    getMessageLayoutConfig(
      message = message,
      agentName = agentName,
      imageHistoryCurIndex = imageHistoryCurIndex,
    )

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = horizontalArrangement,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      // Sender label.
      Text(userLabel, style = MaterialTheme.typography.titleSmall)
    }
  }
}

@Composable
private fun getMessageLayoutConfig(
  message: ChatMessage,
  agentName: String,
  imageHistoryCurIndex: Int,
): MessageLayoutConfig {
  var userLabel = stringResource(R.string.chat_you)
  var rightSideLabel = ""
  var horizontalArrangement = Arrangement.End
  var modifier = Modifier.padding(bottom = 2.dp)

  if (message.side == ChatSide.AGENT) {
    userLabel = agentName
  }

  return MessageLayoutConfig(
    horizontalArrangement = horizontalArrangement,
    modifier = modifier,
    userLabel = userLabel,
    rightSideLabel = rightSideLabel,
  )
}

// @Preview(showBackground = true)
// @Composable
// fun MessageSenderPreview() {
//   LfeTheme {
//     Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp))
// {
//       // Agent message.
//       MessageSender(
//         message = ChatMessageText(content = "hello world", side = ChatSide.AGENT),
//         agentName = stringResource(R.string.chat_generic_agent_name),
//       )
//       // User message.
//       MessageSender(
//         message = ChatMessageText(content = "hello world", side = ChatSide.USER),
//         agentName = stringResource(R.string.chat_generic_agent_name),
//       )
//       // Benchmark during warmup.
//       MessageSender(
//         message =
//           ChatMessageBenchmarkResult(
//             orderedStats = listOf(),
//             statValues = mutableMapOf(),
//             values = listOf(),
//             histogram = Histogram(listOf(), 0),
//             warmupCurrent = 10,
//             warmupTotal = 50,
//             iterationCurrent = 0,
//             iterationTotal = 200,
//           ),
//         agentName = stringResource(R.string.chat_generic_agent_name),
//       )
//       // Benchmark during running.
//       MessageSender(
//         message =
//           ChatMessageBenchmarkResult(
//             orderedStats = listOf(),
//             statValues = mutableMapOf(),
//             values = listOf(),
//             histogram = Histogram(listOf(), 0),
//             warmupCurrent = 50,
//             warmupTotal = 50,
//             iterationCurrent = 123,
//             iterationTotal = 200,
//           ),
//         agentName = stringResource(R.string.chat_generic_agent_name),
//       )
//       // Image generation during running.
//       MessageSender(
//         message =
//           ChatMessageImageWithHistory(
//             bitmaps = listOf(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)),
//             imageBitMaps = listOf(),
//             totalIterations = 10,
//             ChatSide.AGENT,
//           ),
//         agentName = stringResource(R.string.chat_generic_agent_name),
//         imageHistoryCurIndex = 4,
//       )
//     }
//   }
// }

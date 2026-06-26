package com.bradflaugher.lfg.ui.common.chat

// import androidx.compose.ui.tooling.preview.Preview
// import com.bradflaugher.lfg.ui.theme.LfgTheme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import com.bradflaugher.lfg.ui.common.humanReadableDuration

/** Composable function to display the latency of a chat message, if available. */
@Composable
fun LatencyText(message: ChatMessage) {
  val cacheHitText = if (message is ChatMessageText && message.cacheHitPercentage != null) {
    " • Cache Hit: ${(message.cacheHitPercentage!! * 100).toInt()}%"
  } else {
    ""
  }
  if (message.latencyMs >= 0) {
    Text(
      message.latencyMs.humanReadableDuration() + cacheHitText,
      modifier = Modifier.alpha(0.5f).testTag("latency_label"),
      style = MaterialTheme.typography.labelSmall,
    )
  } else if (cacheHitText.isNotEmpty()) {
    Text(
      cacheHitText.removePrefix(" • "),
      modifier = Modifier.alpha(0.5f).testTag("latency_label"),
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

// @Preview(showBackground = true)
// @Composable
// fun LatencyTextPreview() {
//   LfgTheme {
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

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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bradflaugher.lfe.R
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.util.Log
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Language
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color

@Composable
fun ChatHistorySideSheetContent(
  history: List<com.bradflaugher.lfe.proto.ChatSessionProto>,
  onHistoryItemClicked: (String) -> Unit,
  onHistoryItemDeleted: (String) -> Unit,
  onHistoryItemsDeleteAll: () -> Unit,
  onNewChatClicked: () -> Unit,
  onCloseClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var itemToDelete by remember { mutableStateOf<String?>(null) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }
  var showWebLoginDialog by remember { mutableStateOf(false) }

  if (showWebLoginDialog) {
    WebLoginDialog(onDismiss = { showWebLoginDialog = false })
  }

  Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
    // Top Row: Title and Close button
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(stringResource(R.string.chat_history_title), style = MaterialTheme.typography.titleLarge)
      IconButton(onClick = onCloseClicked) {
        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_icon))
      }
    }

    // Actions Row: "+ New chat" pill and "Web Login" pill
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(
        onClick = onNewChatClicked,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
      ) {
        Icon(Icons.Rounded.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.new_chat))
      }

      Button(
        onClick = { showWebLoginDialog = true },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
      ) {
        Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text("Web Login")
      }
    }

    // Subheading Row: Chat history and Clear all button
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        stringResource(R.string.chat_history_title),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextButton(onClick = { showConfirmDeleteDialog = true }) {
        Text(stringResource(R.string.clear_all))
      }
    }

    // History list
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      items(history) { session ->
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onHistoryItemClicked(session.sessionId) }
              .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              session.title,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateStr = formatter.format(Date(session.timestampMs))
            Text(
              "$dateStr • ${session.originalModel}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = { exportChatSession(context, session) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              Icons.Outlined.FileDownload,
              contentDescription = "Export chat history",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { itemToDelete = session.sessionId },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = stringResource(R.string.cd_delete_input_history_entry_icon),
            )
          }
        }
      }
    }
  }

  if (showConfirmDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDeleteDialog = false },
      title = { Text(stringResource(R.string.clear_history_dialog_title)) },
      text = { Text(stringResource(R.string.clear_history_dialog_content)) },
      confirmButton = {
        Button(
          onClick = {
            showConfirmDeleteDialog = false
            onHistoryItemsDeleteAll()
          },
        ) {
          Text(stringResource(R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDeleteDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  if (itemToDelete != null) {
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = { Text(stringResource(R.string.clear_history_dialog_title)) },
      text = { Text(stringResource(R.string.clear_history_dialog_content)) },
      confirmButton = {
        Button(
          onClick = {
            val toDel = itemToDelete
            itemToDelete = null
            if (toDel != null) {
              onHistoryItemDeleted(toDel)
            }
          },
        ) {
          Text(stringResource(R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToDelete = null }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }
}

/**
 * Exports a chat history session to a beautifully formatted Markdown file in the public Downloads folder.
 */
private fun exportChatSession(context: android.content.Context, session: com.bradflaugher.lfe.proto.ChatSessionProto) {
  val title = session.title.replace(Regex("[^a-zA-Z0-9-_ ]"), "_")
  val fileName = "chat_history_${session.sessionId}_$title.md"

  val markdown = StringBuilder().apply {
    append("# Chat Session: ${session.title}\n")
    append("Model: ${session.originalModel}\n")
    append("Date: ${java.util.Date(session.timestampMs)}\n\n")
    append("----------\n\n")
    for (msg in session.messagesList) {
      val sender = when (msg.side) {
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_USER -> "User"
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_MODEL -> "Model"
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_SYSTEM -> "System"
        else -> "Unknown"
      }
      append("**$sender**:\n${msg.content}\n\n")
    }
  }.toString()

  try {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
      put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
      resolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(markdown.toByteArray())
      }
      Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
    } else {
      throw Exception("Could not create MediaStore entry")
    }
  } catch (e: Exception) {
    Log.e("ChatExporter", "Failed to export chat", e)
    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
  }
}

/**
 * A beautiful, full-screen dialog containing an interactive WebView browser.
 * Enables users to log in to subscription or paywalled sites (e.g., NYT, WSJ, Bloomberg)
 * within the application process. Session cookies are automatically stored in the shared
 * CookieManager, allowing background article summarize/finder tasks to read full contents.
 */
@Composable
fun WebLoginDialog(
  onDismiss: () -> Unit
) {
  var urlInput by remember { mutableStateOf("https://www.nytimes.com") }
  var currentUrl by remember { mutableStateOf("https://www.nytimes.com") }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  androidx.compose.ui.window.Dialog(
    onDismissRequest = onDismiss,
    properties = androidx.compose.ui.window.DialogProperties(
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Top row: Title and Done button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Web Session Login",
            style = MaterialTheme.typography.titleLarge
          )
          TextButton(onClick = onDismiss) {
            Text("Done")
          }
        }

        Text(
          text = "Sign in to any subscription or paywalled website below. The agent will share your browser cookies to fetch full articles.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // URL Input and Go button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("URL") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          Button(onClick = {
            var target = urlInput.trim()
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
              target = "https://$target"
            }
            currentUrl = target
            webViewRef?.loadUrl(target)
          }) {
            Text("Go")
          }
        }

        // Quick Shortcuts Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val sites = listOf(
            "NYT" to "https://www.nytimes.com",
            "WSJ" to "https://www.wsj.com",
            "Bloomberg" to "https://www.bloomberg.com",
            "FT" to "https://www.ft.com"
          )
          for ((name, targetUrl) in sites) {
            Button(
              onClick = {
                urlInput = targetUrl
                currentUrl = targetUrl
                webViewRef?.loadUrl(targetUrl)
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(32.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              )
            ) {
              Text(name, style = MaterialTheme.typography.labelSmall)
            }
          }
        }

        // WebView rendering
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
        ) {
          AndroidView(
            factory = { ctx ->
              WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                
                android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                      urlInput = it
                      currentUrl = it
                    }
                  }
                }
                loadUrl(currentUrl)
              }
            },
            update = {
              // Navigation handled inside factory/buttons
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

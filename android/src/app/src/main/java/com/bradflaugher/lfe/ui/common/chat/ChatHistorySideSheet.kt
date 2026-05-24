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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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

    // Actions Row: "+ New chat" pill
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.Start,
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



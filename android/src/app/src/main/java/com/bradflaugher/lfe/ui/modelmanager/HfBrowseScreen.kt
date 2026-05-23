/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.ui.modelmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bradflaugher.lfe.data.HfModelInfo
import com.bradflaugher.lfe.data.HfSibling
import java.util.Locale

/**
 * HuggingFace model browser. Searches `huggingface.co/api/models`, lets the user pick a model
 * file, and downloads it via the existing [ModelManagerViewModel.downloadFromHuggingFace] path.
 *
 * The default landing view shows the `litert-community` org sorted by downloads so the first
 * thing the user sees is something that actually runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HfBrowseScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  viewModel: HfBrowseViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsState()

  val visible =
    remember(state.results, state.liteRtOnly) {
      if (state.liteRtOnly) state.results.filter { it.isLfeCompatible } else state.results
    }

  val listState = rememberLazyListState()
  val shouldLoadMore by remember {
    derivedStateOf {
      val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
      last >= visible.size - 3 && state.nextPageUrl != null && !state.loadingMore && !state.loading
    }
  }
  LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Browse HuggingFace") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.retry() }) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
          }
        },
      )
    },
  ) { inner ->
    Column(modifier = Modifier.fillMaxSize().padding(inner)) {
      OutlinedTextField(
        value = state.query,
        onValueChange = viewModel::onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search HuggingFace…") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
      )

      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = state.liteRtOnly,
          onClick = { viewModel.toggleLiteRtOnly() },
          label = { Text("LiteRT-LM only") },
        )
        Spacer(Modifier.size(0.dp))
        Text(
          "${visible.size} models",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      when {
        state.loading -> CenteredSpinner()
        state.error != null -> ErrorPanel(state.error!!, onRetry = viewModel::retry)
        visible.isEmpty() ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              if (state.liteRtOnly) {
                "No LiteRT-LM-compatible models matched. Try toggling the filter off."
              } else {
                "No models matched. Try a broader search."
              },
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(32.dp),
            )
          }
        else ->
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(visible, key = { it.id }) { repo ->
              HfRepoCard(
                repo = repo,
                onDownload = { sibling ->
                  modelManagerViewModel.downloadFromHuggingFace(
                    repoId = repo.id,
                    fileName = sibling.rfilename,
                    sizeInBytes = sibling.size ?: 0L,
                    supportImage = repo.looksMultimodal(),
                    supportAudio = repo.tags.any { it.contains("audio", ignoreCase = true) },
                  )
                  navigateUp()
                },
              )
            }
            if (state.loadingMore) {
              item { CenteredSpinner() }
            }
          }
      }
    }
  }
}

@Composable
private fun HfRepoCard(
  repo: HfModelInfo,
  onDownload: (HfSibling) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        repo.id,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (repo.downloads > 0) {
          Text(
            "↓ ${humanCount(repo.downloads)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (repo.likes > 0) {
          Text(
            "♥ ${humanCount(repo.likes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (repo.gated != null && repo.gated != false) {
          Text(
            "gated",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
      Spacer(Modifier.height(8.dp))

      val runtimeFiles = repo.runtimeFiles
      if (runtimeFiles.isEmpty()) {
        Text(
          "No LiteRT-LM file in this repo — not runnable in LFE.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      } else {
        runtimeFiles.forEach { sibling ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(sibling.rfilename, style = MaterialTheme.typography.bodyMedium)
              sibling.size?.let {
                Text(
                  humanBytes(it),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            TextButton(onClick = { onDownload(sibling) }) {
              Icon(
                Icons.Rounded.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.size(4.dp))
              Text("Download")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CenteredSpinner() {
  Box(
    modifier = Modifier.fillMaxWidth().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) { CircularProgressIndicator() }
}

@Composable
private fun ErrorPanel(
  message: String,
  onRetry: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(message, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onRetry) { Text("Retry") }
  }
}

private fun HfModelInfo.looksMultimodal(): Boolean {
  val tagsLower = tags.map { it.lowercase() }
  return tagsLower.any { it.contains("vision") || it.contains("multimodal") || it.contains("image") }
}

internal fun humanBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val units = arrayOf("KB", "MB", "GB", "TB")
  var v = bytes.toDouble() / 1024
  var idx = 0
  while (v >= 1024 && idx < units.size - 1) {
    v /= 1024
    idx++
  }
  return String.format(Locale.US, "%.1f %s", v, units[idx])
}

internal fun humanCount(n: Long): String {
  return when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
  }
}

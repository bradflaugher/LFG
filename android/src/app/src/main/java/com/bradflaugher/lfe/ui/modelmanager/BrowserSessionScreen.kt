/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.ui.modelmanager

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A plain WebView screen for signing in to a paywalled site (WSJ, NYT, etc.). After login,
 * the session cookies are kept by Android's [CookieManager] across app restarts and reused
 * automatically when the agent's `fetchArticle` tool later loads a URL from the same domain.
 *
 * No publisher-specific UI — the user types any URL (or just `wsj.com`) and signs in there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserSessionScreen(navigateUp: () -> Unit) {
  var urlField by remember { mutableStateOf("https://") }
  var loadedUrl by remember { mutableStateOf<String?>(null) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Browser session") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (loadedUrl != null) {
            TextButton(onClick = navigateUp) { Text("Done") }
          }
        },
      )
    },
  ) { inner ->
    Column(
      modifier = Modifier.fillMaxSize().padding(inner),
      verticalArrangement = Arrangement.Top,
    ) {
      OutlinedTextField(
        value = urlField,
        onValueChange = { urlField = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        label = { Text("URL (e.g. https://www.wsj.com)") },
        singleLine = true,
        keyboardActions =
          androidx.compose.foundation.text.KeyboardActions(
            onGo = {
              val u = normalizeUrl(urlField)
              urlField = u
              loadedUrl = u
              webViewRef?.loadUrl(u)
            },
          ),
        keyboardOptions =
          androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
        trailingIcon = {
          TextButton(onClick = {
            val u = normalizeUrl(urlField)
            urlField = u
            loadedUrl = u
            webViewRef?.loadUrl(u)
          }) {
            Text("Go")
          }
        },
      )

      Text(
        "Sign in here — LFE keeps the cookies on-device. Later, the " +
          "`summarize-article` skill reuses this session to fetch paywalled URLs.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      )

      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString =
              "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = WebViewClient()
            webViewRef = this
          }
        },
      )
    }
  }
}

internal fun normalizeUrl(input: String): String {
  val trimmed = input.trim()
  return when {
    trimmed.isEmpty() -> "https://"
    trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
    else -> "https://$trimmed"
  }
}

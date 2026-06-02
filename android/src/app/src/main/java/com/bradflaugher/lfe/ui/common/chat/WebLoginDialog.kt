/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager

object WebLoginHelper {
  fun normalizeUrl(input: String): String {
    val target = input.trim()
    if (target.isEmpty()) return ""
    if (!target.startsWith("http://") && !target.startsWith("https://")) {
      return "https://$target"
    }
    return target
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
  val context = LocalContext.current
  var urlInput by remember { mutableStateOf("https://www.google.com") }
  var currentUrl by remember { mutableStateOf("https://www.google.com") }
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
        // Top row: Title, Clear Cookies, and Done buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Web Browser",
            style = MaterialTheme.typography.titleLarge
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(
              onClick = {
                CookieManager.getInstance().removeAllCookies { success ->
                  CookieManager.getInstance().flush()
                  webViewRef?.reload()
                  Toast.makeText(context, "Cookies cleared successfully", Toast.LENGTH_SHORT).show()
                }
              },
              colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
              )
            ) {
              Text("Clear Cookies")
            }
            TextButton(onClick = onDismiss) {
              Text("Done")
            }
          }
        }

        Text(
          text = "Browse and sign in to any website. The agent will share your browser session and cookies to retrieve full web content.",
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
            val target = WebLoginHelper.normalizeUrl(urlInput)
            if (target.isNotEmpty()) {
              currentUrl = target
              webViewRef?.loadUrl(target)
            }
          }) {
            Text("Go")
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
                // SECURITY: Disable direct file access to prevent path traversal / local file inclusion vulnerabilities.
                settings.allowFileAccess = false
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

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

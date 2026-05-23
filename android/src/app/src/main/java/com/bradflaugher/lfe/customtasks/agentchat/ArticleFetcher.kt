/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Bundles Mozilla Readability.js (Apache 2.0) at runtime — see
 * assets/js/readability.js.
 */
package com.bradflaugher.lfe.customtasks.agentchat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AGArticleFetcher"

private const val DEFAULT_USER_AGENT =
  "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

private const val LOAD_TIMEOUT_MS = 30_000L
private const val EXTRACT_TIMEOUT_MS = 10_000L

/**
 * Fetches an article URL in a hidden [WebView] (so Android's [CookieManager] attaches any
 * cookies from prior in-app sign-ins) and runs Mozilla Readability.js to extract main content.
 *
 * Returns a JSON string. On success: `{"title": "...", "text": "...", "url": "..."}`.
 * On failure: `{"error": "..."}`.
 *
 * Must be invoked from the main thread because [WebView] is Activity-bound.
 */
object ArticleFetcher {
  @SuppressLint("SetJavaScriptEnabled")
  suspend fun fetch(context: Context, url: String): String =
    withContext(Dispatchers.Main) {
      val readabilityJs =
        try {
          context.assets.open("js/readability.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
          Log.e(TAG, "Could not load Readability.js", e)
          return@withContext errorJson("Readability bundle missing: ${e.message}")
        }

      try {
        withTimeout(LOAD_TIMEOUT_MS + EXTRACT_TIMEOUT_MS + 2_000L) {
          fetchInternal(context, url, readabilityJs)
        }
      } catch (e: TimeoutCancellationException) {
        errorJson("Timed out fetching $url")
      } catch (e: Exception) {
        Log.e(TAG, "fetch failed", e)
        errorJson(e.message ?: "fetch failed")
      }
    }

  private suspend fun fetchInternal(
    context: Context,
    url: String,
    readabilityJs: String,
  ): String = suspendCancellableCoroutine { cont ->
    val webView = WebView(context).apply {
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.loadsImagesAutomatically = false
      settings.blockNetworkImage = true
      settings.userAgentString = DEFAULT_USER_AGENT
      settings.cacheMode = WebSettings.LOAD_DEFAULT
    }
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    var resumed = false
    fun safeResume(result: String) {
      if (!resumed) {
        resumed = true
        try {
          webView.stopLoading()
        } catch (_: Exception) {}
        webView.destroy()
        cont.resume(result)
      }
    }
    fun safeFail(t: Throwable) {
      if (!resumed) {
        resumed = true
        webView.destroy()
        cont.resumeWithException(t)
      }
    }

    cont.invokeOnCancellation {
      if (!resumed) {
        resumed = true
        try {
          webView.destroy()
        } catch (_: Exception) {}
      }
    }

    webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView, finishedUrl: String) {
        val extract = """
          (function() {
            try {
              $readabilityJs
              var doc = document.cloneNode(true);
              var article = new Readability(doc).parse();
              if (!article) {
                return JSON.stringify({error: 'Readability returned no article'});
              }
              return JSON.stringify({
                title: article.title || '',
                text: (article.textContent || '').trim(),
                url: window.location.href,
              });
            } catch (e) {
              return JSON.stringify({error: 'Readability threw: ' + (e && e.message)});
            }
          })();
        """.trimIndent()
        try {
          view.evaluateJavascript(extract) { rawJsResult ->
            // evaluateJavascript wraps strings in another set of quotes — unquote them.
            val unwrapped = unwrapJsString(rawJsResult)
            safeResume(unwrapped ?: errorJson("Empty extractor result"))
          }
        } catch (e: Exception) {
          safeFail(e)
        }
      }

      override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError,
      ) {
        if (request.isForMainFrame) {
          safeResume(errorJson("WebView error: ${error.description} (code ${error.errorCode})"))
        }
      }
    }

    webView.loadUrl(url)
  }

  internal fun errorJson(message: String): String {
    // Pure-Kotlin JSON escape so this stays testable without the org.json stub. Order matters:
    // backslash first, then quote, then control chars.
    val escaped = message
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
    return "{\"error\":\"$escaped\"}"
  }

  /**
   * `WebView.evaluateJavascript` wraps the returned value as a JS literal: a JS string becomes
   * `"\"actual\""`. Strip the outer pair of quotes and decode the standard `\"`, `\\`, `\n` etc.
   * escapes. Returns null for the literal `null` (signaling "JS returned no value").
   */
  internal fun unwrapJsString(raw: String?): String? {
    if (raw == null || raw == "null") return null
    if (raw.length < 2 || !raw.startsWith("\"") || !raw.endsWith("\"")) return raw
    val inner = raw.substring(1, raw.length - 1)
    val out = StringBuilder(inner.length)
    var i = 0
    while (i < inner.length) {
      val c = inner[i]
      if (c == '\\' && i + 1 < inner.length) {
        when (val next = inner[i + 1]) {
          '"', '\\', '/' -> out.append(next)
          'n' -> out.append('\n')
          'r' -> out.append('\r')
          't' -> out.append('\t')
          'b' -> out.append('\b')
          'f' -> out.append('')
          'u' ->
            if (i + 5 < inner.length) {
              val hex = inner.substring(i + 2, i + 6)
              out.append(hex.toInt(16).toChar())
              i += 4
            } else {
              out.append('\\').append(next)
            }
          else -> out.append('\\').append(next)
        }
        i += 2
      } else {
        out.append(c)
        i++
      }
    }
    return out.toString()
  }
}

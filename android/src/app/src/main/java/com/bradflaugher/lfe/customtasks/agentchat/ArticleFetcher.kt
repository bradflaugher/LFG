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
 * Hidden [WebView] helpers that share one piece of plumbing: load a URL with the app's
 * [CookieManager] attached, wait for `onPageFinished`, then run a JS snippet to extract data.
 *
 * - [fetch] runs Mozilla Readability.js to extract a full article body. The bundle is injected
 *   as its own `evaluateJavascript` call so a single byte of stray syntax in the (huge) bundle
 *   can't take down the extractor IIFE with it.
 * - [fetchLinks] extracts a deduped list of in-page anchor links (used to surface article
 *   candidates from a homepage like nytimes.com).
 *
 * Both must be invoked from the main thread because [WebView] is Activity-bound.
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

      val extractor = """
        (function() {
          try {
            if (typeof Readability !== 'function') {
              return JSON.stringify({error: 'Readability bundle did not load'});
            }
            var doc = document.cloneNode(true);
            var article = new Readability(doc).parse();
            if (!article || !(article.textContent || '').trim()) {
              return JSON.stringify({error: 'No readable article body found at ' + window.location.href});
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

      runWithWebView(
        context = context,
        url = url,
        preScripts = listOf(readabilityJs),
        script = extractor,
      )
    }

  /**
   * Returns a JSON string with a deduplicated list of in-page links. Filters out boilerplate
   * (nav items, login buttons) using a text-length window suitable for article headlines.
   *
   * On success: `{"url": "...", "links": [{"text": "...", "href": "..."}]}`.
   * On failure: `{"error": "..."}`.
   */
  @SuppressLint("SetJavaScriptEnabled")
  suspend fun fetchLinks(context: Context, url: String): String =
    withContext(Dispatchers.Main) {
      val script = """
        (function() {
          try {
            var rows = Array.prototype.map.call(
              document.querySelectorAll('a[href]'),
              function(a) {
                var text = (a.textContent || '').replace(/\s+/g, ' ').trim();
                var href = a.href || '';
                return {text: text, href: href};
              }
            );
            var seen = {};
            var out = [];
            for (var i = 0; i < rows.length; i++) {
              var r = rows[i];
              if (!r.href || r.href.indexOf('http') !== 0) continue;
              if (r.href.indexOf('#') === r.href.length - 1) continue;
              if (r.text.length < 20 || r.text.length > 220) continue;
              if (seen[r.href]) continue;
              seen[r.href] = true;
              out.push(r);
              if (out.length >= 80) break;
            }
            return JSON.stringify({url: window.location.href, links: out});
          } catch (e) {
            return JSON.stringify({error: 'link extractor threw: ' + (e && e.message)});
          }
        })();
      """.trimIndent()

      runWithWebView(context = context, url = url, preScripts = emptyList(), script = script)
    }

  private suspend fun runWithWebView(
    context: Context,
    url: String,
    preScripts: List<String>,
    script: String,
  ): String =
    try {
      withTimeout(LOAD_TIMEOUT_MS + EXTRACT_TIMEOUT_MS + 2_000L) {
        runWithWebViewInternal(context, url, preScripts, script)
      }
    } catch (e: TimeoutCancellationException) {
      errorJson("Timed out fetching $url")
    } catch (e: Exception) {
      Log.e(TAG, "WebView fetch failed", e)
      errorJson(e.message ?: "fetch failed")
    }

  @SuppressLint("SetJavaScriptEnabled")
  private suspend fun runWithWebViewInternal(
    context: Context,
    url: String,
    preScripts: List<String>,
    script: String,
  ): String = suspendCancellableCoroutine { cont ->
    // Many news articles need images enabled (Readability looks at <img> for caption text)
    // and rely on a tiny amount of client-side rendering. We keep network image blocking off
    // to avoid blank-body cases that previously surfaced to the user as "skill is broken".
    val webView = WebView(context).apply {
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.loadsImagesAutomatically = true
      settings.blockNetworkImage = false
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
      private var ran = false

      override fun onPageFinished(view: WebView, finishedUrl: String) {
        // Some pages fire onPageFinished multiple times (e.g. iframes, soft-nav). Only
        // attempt extraction once per fetch.
        if (ran) return
        ran = true

        // Give client-side rendered articles a moment to populate the DOM before we try to
        // extract. Hard-coded short wait — anything longer hurts the common case.
        view.postDelayed({
          try {
            // 1) Inject every preScript in order. We discard their return values; their job
            //    is only to define globals (e.g. Readability).
            val preIter = preScripts.iterator()
            fun runNext() {
              if (preIter.hasNext()) {
                view.evaluateJavascript(preIter.next()) { _ -> runNext() }
              } else {
                view.evaluateJavascript(script) { raw ->
                  safeResume(unwrapJsString(raw) ?: errorJson("Empty extractor result"))
                }
              }
            }
            runNext()
          } catch (e: Exception) {
            safeFail(e)
          }
        }, 1500L)
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

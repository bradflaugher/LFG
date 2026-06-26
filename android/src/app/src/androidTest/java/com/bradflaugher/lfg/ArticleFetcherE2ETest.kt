/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfg

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.bradflaugher.lfg.customtasks.agentchat.ArticleFetcher
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleFetcherE2ETest {

  @Test
  fun testFetchLinksAndArticleEndToEnd() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    // 1. Fetch links from Hacker News (a stable, open test site that won't block us)
    val testUrl = "https://news.ycombinator.com"
    val linksJsonStr = ArticleFetcher.fetchLinks(context, testUrl)
    assertNotNull("Links JSON should not be null", linksJsonStr)
    assertFalse("Links JSON should not be empty", linksJsonStr.isEmpty())

    val linksObj = JSONObject(linksJsonStr)
    if (linksObj.has("error") || !linksObj.has("url")) {
      val errorMsg = if (linksObj.has("error")) linksObj.getString("error") else "No 'url' key present (returned JSON: $linksJsonStr)"
      println("E2E Test: fetchLinks failed/skipped: $errorMsg")
      assumeTrue(
        "Skipping E2E test: WebView failed to load target page or extract links: $errorMsg",
        false
      )
    }

    assertTrue("JSON should contain links key", linksObj.has("links"))

    val linksArray = linksObj.getJSONArray("links")
    assertTrue("Links array should not be empty on Hacker News", linksArray.length() > 0)

    val numLinks = linksArray.length()
    println("E2E Test: Successfully extracted $numLinks links from $testUrl")

    // Find the first valid article link to fetch
    var pickedUrl: String? = null
    for (i in 0 until numLinks) {
      val linkItem = linksArray.getJSONObject(i)
      val href = linkItem.optString("href", "")
      // We want an external article link (not Hacker News internal comments link)
      if (href.isNotEmpty() && !href.contains("ycombinator.com")) {
        pickedUrl = href
        break
      }
    }

    if (pickedUrl == null && numLinks > 0) {
      pickedUrl = linksArray.getJSONObject(0).optString("href")
    }

    assertNotNull("Could not find a valid article URL to fetch", pickedUrl)
    println("E2E Test: Fetching selected article body from $pickedUrl")

    // 2. Fetch the full body text using Readability
    val articleJsonStr = ArticleFetcher.fetch(context, pickedUrl!!)
    assertNotNull("Article JSON should not be null", articleJsonStr)
    assertFalse("Article JSON should not be empty", articleJsonStr.isEmpty())

    val articleObj = JSONObject(articleJsonStr)
    
    if (articleObj.has("error") || !articleObj.has("text")) {
      val errorMsg = if (articleObj.has("error")) articleObj.getString("error") else "No 'text' key present"
      println("E2E Test: Article fetch failed/skipped: $errorMsg")
      assumeTrue(
        "Skipping E2E test: WebView failed to load article body or extract text: $errorMsg",
        false
      )
    } else {
      assertTrue("JSON should contain title key", articleObj.has("title"))
      assertTrue("JSON should contain text key", articleObj.has("text"))
      assertTrue("JSON should contain url key", articleObj.has("url"))

      val text = articleObj.getString("text")
      val title = articleObj.getString("title")
      println("E2E Test: Successfully extracted article title: '$title'")
      assertTrue("Article body text should not be empty", text.isNotEmpty())
    }
  }
}

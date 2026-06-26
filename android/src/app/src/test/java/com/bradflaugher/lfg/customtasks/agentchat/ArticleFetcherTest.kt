/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 *
 * The WebView dance in [ArticleFetcher] needs a real Activity to test end-to-end —
 * that's covered by the instrumented suite. These tests only exercise the pure
 * helper bits (JSON unwrap, error JSON shape).
 */
package com.bradflaugher.lfg.customtasks.agentchat

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleFetcherTest {
  @Test fun unwrapJsStringHandlesQuotedString() {
    assertEquals("hello", ArticleFetcher.unwrapJsString("\"hello\""))
  }

  @Test fun unwrapJsStringDecodesEscapedQuotesAndBackslashes() {
    // evaluateJavascript wraps a JSON-containing string: `{"a":1}` arrives as `"{\"a\":1}"`.
    val rawFromJs = "\"{\\\"a\\\":1}\""
    assertEquals("{\"a\":1}", ArticleFetcher.unwrapJsString(rawFromJs))
  }

  @Test fun unwrapJsStringDecodesNewlineEscape() {
    assertEquals("a\nb", ArticleFetcher.unwrapJsString("\"a\\nb\""))
  }

  @Test fun unwrapJsStringPassesThroughLiteralNullAndPlainValues() {
    assertNull(ArticleFetcher.unwrapJsString(null))
    assertNull(ArticleFetcher.unwrapJsString("null"))
    // A non-quoted token (a number, an array literal) is returned unchanged.
    assertEquals("42", ArticleFetcher.unwrapJsString("42"))
    assertEquals("[1,2]", ArticleFetcher.unwrapJsString("[1,2]"))
  }

  @Test fun errorJsonIsValidJsonWithEscaping() {
    // Use Gson (a real parser) instead of org.json (stubbed in unit tests).
    val parsed = Gson().fromJson(ArticleFetcher.errorJson("kaboom \"quote\" \n nl"), Map::class.java)
    assertTrue(parsed.containsKey("error"))
    assertEquals("kaboom \"quote\" \n nl", parsed["error"])
    assertEquals(1, parsed.size)
  }
}

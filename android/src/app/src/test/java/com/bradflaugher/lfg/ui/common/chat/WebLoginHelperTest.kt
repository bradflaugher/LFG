package com.bradflaugher.lfg.ui.common.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for [WebLoginHelper] URL normalization functions.
 * Validates protocol prepending, whitespace trimming, and empty states.
 */
class WebLoginHelperTest {

  @Test
  fun normalizeUrl_emptyInput_returnsEmptyString() {
    assertEquals("", WebLoginHelper.normalizeUrl(""))
    assertEquals("", WebLoginHelper.normalizeUrl("   "))
  }

  @Test
  fun normalizeUrl_alreadyHasHttps_returnsAsIs() {
    val input = "https://www.nytimes.com"
    assertEquals(input, WebLoginHelper.normalizeUrl(input))
  }

  @Test
  fun normalizeUrl_alreadyHasHttp_returnsAsIs() {
    val input = "http://example.com"
    assertEquals(input, WebLoginHelper.normalizeUrl(input))
  }

  @Test
  fun normalizeUrl_missingPrefix_prependsHttps() {
    assertEquals("https://nytimes.com", WebLoginHelper.normalizeUrl("nytimes.com"))
    assertEquals("https://www.wsj.com", WebLoginHelper.normalizeUrl("www.wsj.com"))
  }

  @Test
  fun normalizeUrl_trimsWhitespace() {
    assertEquals("https://nytimes.com", WebLoginHelper.normalizeUrl("   nytimes.com   "))
    assertEquals("https://nytimes.com", WebLoginHelper.normalizeUrl("\tnytimes.com\n"))
    assertEquals("https://nytimes.com", WebLoginHelper.normalizeUrl(" https://nytimes.com "))
  }
}

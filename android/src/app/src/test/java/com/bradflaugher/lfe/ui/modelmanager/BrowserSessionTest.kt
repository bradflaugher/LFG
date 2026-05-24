/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.ui.modelmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserSessionTest {
  @Test fun normalizeUrlAddsHttpsWhenSchemeMissing() {
    assertEquals("https://wsj.com", normalizeUrl("wsj.com"))
    assertEquals("https://www.nytimes.com", normalizeUrl("www.nytimes.com"))
  }

  @Test fun normalizeUrlPreservesExistingScheme() {
    assertEquals("http://example.com", normalizeUrl("http://example.com"))
    assertEquals("https://example.com/path?q=1", normalizeUrl("https://example.com/path?q=1"))
  }

  @Test fun normalizeUrlTrimsWhitespace() {
    assertEquals("https://wsj.com", normalizeUrl("  wsj.com  "))
  }

  @Test fun normalizeUrlReturnsHttpsForEmpty() {
    assertEquals("https://", normalizeUrl(""))
    assertEquals("https://", normalizeUrl("   "))
  }
}

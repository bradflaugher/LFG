/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuggingFaceApiTest {
  @Test fun parsesHfModelsResponseFromCapturedJson() {
    val json = """
      [
        {
          "id": "litert-community/Gemma3-1B-IT",
          "author": "litert-community",
          "downloads": 12345,
          "likes": 42,
          "lastModified": "2025-09-01T00:00:00.000Z",
          "tags": ["text-generation", "litert"],
          "siblings": [
            {"rfilename": "gemma3-1b-it-int4.litertlm", "size": 584417280},
            {"rfilename": "README.md"}
          ]
        },
        {
          "id": "meta-llama/Llama-4-8B",
          "author": "meta-llama",
          "downloads": 99,
          "likes": 0,
          "tags": ["text-generation"],
          "siblings": [
            {"rfilename": "model.safetensors"}
          ]
        }
      ]
    """.trimIndent()
    val parsed = Gson().fromJson(json, Array<HfModelInfo>::class.java).toList()
    assertEquals(2, parsed.size)
    assertEquals("litert-community/Gemma3-1B-IT", parsed[0].id)
    assertEquals(12345L, parsed[0].downloads)
    assertEquals(2, parsed[0].siblings.size)
  }

  @Test fun isLfeCompatibleAcceptsLitertlmAndTaskFiles() {
    val good = HfModelInfo(
      id = "x/y",
      siblings = listOf(HfSibling("model.litertlm", 1000)),
    )
    val alsoGood = HfModelInfo(
      id = "x/y",
      siblings = listOf(HfSibling("model.task", 1000)),
    )
    val bad = HfModelInfo(
      id = "x/y",
      siblings = listOf(HfSibling("model.safetensors", 1000)),
    )
    val empty = HfModelInfo(id = "x/y", siblings = emptyList())

    assertTrue(good.isLfeCompatible)
    assertTrue(alsoGood.isLfeCompatible)
    assertFalse(bad.isLfeCompatible)
    assertFalse(empty.isLfeCompatible)
  }

  @Test fun runtimeFilesExcludesNonRuntimeSiblings() {
    val repo = HfModelInfo(
      id = "x/y",
      siblings = listOf(
        HfSibling("README.md"),
        HfSibling("model.litertlm", 100),
        HfSibling("config.json"),
        HfSibling("model.task", 200),
      ),
    )
    val runtime = repo.runtimeFiles
    assertEquals(2, runtime.size)
    assertEquals(setOf("model.litertlm", "model.task"), runtime.map { it.rfilename }.toSet())
  }

  @Test fun parseNextLinkHeaderHandlesStandardForm() {
    val header = "<https://huggingface.co/api/models?cursor=abc>; rel=\"next\""
    assertEquals(
      "https://huggingface.co/api/models?cursor=abc",
      HuggingFaceApi.parseNextLinkHeader(header),
    )
  }

  @Test fun parseNextLinkHeaderReturnsNullForEmpty() {
    assertNull(HuggingFaceApi.parseNextLinkHeader(null))
    assertNull(HuggingFaceApi.parseNextLinkHeader(""))
  }

  @Test fun buildSearchUrlEncodesParamsAndOrdersThem() {
    val url = HuggingFaceApi.buildSearchUrl(
      search = "gemma 4",
      author = null,
      filter = null,
      sort = "downloads",
      direction = "-1",
      limit = 25,
      full = true,
    )
    assertNotNull(url)
    assertTrue(url.startsWith("https://huggingface.co/api/models?"))
    assertTrue(url.contains("search=gemma+4") || url.contains("search=gemma%204"))
    assertTrue(url.contains("sort=downloads"))
    assertTrue(url.contains("limit=25"))
    assertTrue(url.contains("full=true"))
    assertFalse(url.contains("author="))
  }
}

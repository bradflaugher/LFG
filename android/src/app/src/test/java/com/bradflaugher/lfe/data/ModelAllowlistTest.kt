/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfe.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelAllowlistTest {
  private val removedTaskTypes = setOf(
    "llm_chat",
    "llm_prompt_lab",
    "llm_ask_image",
    "llm_ask_audio",
    "llm_mobile_actions",
    "llm_tiny_garden",
    "mp_scrapbook",
  )

  private fun loadBundled(): ModelAllowlist {
    val json =
      javaClass.classLoader!!.getResourceAsStream("model_allowlist.json")
        .bufferedReader()
        .use { it.readText() }
    val allowlist = Gson().fromJson(json, ModelAllowlist::class.java)
    assertNotNull("allowlist parsed", allowlist)
    return allowlist
  }

  @Test fun bundledAllowlistParsesAndIsNonEmpty() {
    val allowlist = loadBundled()
    assertTrue("at least one model", allowlist.models.isNotEmpty())
  }

  @Test fun everyModelTargetsAgentChatOnly() {
    val allowlist = loadBundled()
    for (model in allowlist.models) {
      assertEquals(
        "${model.name} should target exactly LLM_AGENT_CHAT",
        listOf(BuiltInTaskId.LLM_AGENT_CHAT),
        model.taskTypes,
      )
    }
  }

  @Test fun noModelReferencesRemovedTaskTypes() {
    val allowlist = loadBundled()
    for (model in allowlist.models) {
      for (t in model.taskTypes) {
        assertFalse("${model.name} still references removed task type $t", t in removedTaskTypes)
      }
      model.bestForTaskTypes?.forEach { t ->
        assertFalse("${model.name} bestFor references removed task type $t", t in removedTaskTypes)
      }
      model.capabilityToTaskTypes?.values?.flatten()?.forEach { t ->
        assertFalse(
          "${model.name} capabilityToTaskTypes references removed task type $t",
          t in removedTaskTypes,
        )
      }
    }
  }

  @Test fun builtInTaskIdHasOnlyAgentChat() {
    val knownIds = BuiltInTaskId::class.java.declaredFields
      .filter { it.type == String::class.java }
      .map { it.get(null) as String }
      .toSet()
    assertEquals(
      "BuiltInTaskId should expose only LLM_AGENT_CHAT",
      setOf(BuiltInTaskId.LLM_AGENT_CHAT),
      knownIds,
    )
  }
}

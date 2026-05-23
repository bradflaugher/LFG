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
package com.bradflaugher.lfe.customtasks.agentchat

import com.bradflaugher.lfe.proto.Skill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatTaskTest {
  @Test fun defaultSystemPromptIsSkillsOnly() {
    val prompt = DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED
    assertTrue("prompt mentions skills", prompt.contains("___SKILLS___"))
    assertFalse("prompt should not contain MCP placeholder", prompt.contains("___TOOLS___"))
    assertFalse("prompt should not mention MCP", prompt.contains("MCP", ignoreCase = false))
  }

  @Test fun isDefaultSystemPromptMatchesSkillsOnly() {
    assertTrue(isDefaultSystemPrompt(DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED))
    assertFalse(isDefaultSystemPrompt("anything else"))
  }

  @Test fun getEffectiveBaseSystemPromptPassesThroughCustom() {
    assertEquals(
      DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED,
      getEffectiveBaseSystemPrompt(DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED),
    )
    assertEquals(
      "custom prompt",
      getEffectiveBaseSystemPrompt("custom prompt"),
    )
  }

  @Test fun injectSkillsReturnsEmptyWhenNoSkillsSelected() {
    val result = injectSkills(
      baseSystemPrompt = DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED,
      skills = listOf(),
    )
    assertEquals("", result.toString())
  }

  @Test fun injectSkillsSubstitutesSelectedSkills() {
    val skills = listOf(
      Skill.newBuilder().setName("translator").setDescription("Translate text").setSelected(true).build(),
      Skill.newBuilder().setName("calculate-hash").setDescription("Hash text").setSelected(false).build(),
      Skill.newBuilder().setName("qr-code").setDescription("Make a QR").setSelected(true).build(),
    )
    val result = injectSkills(
      baseSystemPrompt = "PROMPT WITH ___SKILLS___ MARKER",
      skills = skills,
    ).toString()
    assertTrue("includes translator", result.contains("translator"))
    assertTrue("includes qr-code", result.contains("qr-code"))
    assertFalse(
      "excludes unselected calculate-hash",
      result.contains("calculate-hash"),
    )
    assertFalse("marker substituted", result.contains("___SKILLS___"))
  }
}

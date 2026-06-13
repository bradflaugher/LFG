/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledSkillsTest {
  private val expected =
    setOf(
      // Privacy power-tools
      "redact",
      "explain-document",
      "scam-check",
      "privacy-lens",
      // Uncensored (best with an Abliterated model)
      "straight-answer",
      "unfiltered-muse",
      "roleplay",
      "devils-advocate",
      // Your data stays on the device
      "private-journal",
      "mood-tracker",
      "quick-note",
      "budget-tracker",
      // Offline / personal
      "password-generator",
      "translator",
      "set-reminder",
      "whats-on-my-calendar",
    )

  private val skillsRoot = File("src/main/assets/skills")

  @Test fun bundledSkillSetMatchesCuratedSet() {
    if (!skillsRoot.exists()) return // run from app/ dir; sandboxed runs may skip
    val actual = skillsRoot.listFiles { f -> f.isDirectory }!!.map { it.name }.toSet()
    assertEquals(expected, actual)
  }

  @Test fun everyBundledSkillHasSkillMd() {
    if (!skillsRoot.exists()) return
    skillsRoot.listFiles { f -> f.isDirectory }!!.forEach { dir ->
      val skillMd = File(dir, "SKILL.md")
      assertTrue("${dir.name} missing SKILL.md", skillMd.exists())
      val text = skillMd.readText()
      assertTrue("${dir.name} missing name frontmatter", text.contains("name:"))
      assertTrue("${dir.name} missing description frontmatter", text.contains("description:"))
    }
  }
}

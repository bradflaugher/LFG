/*
 * LFE — Apache 2.0.
 *
 * Verifies the bundled skills folder:
 *   1. exactly the curated set of 9 skills is present
 *   2. each skill directory has a SKILL.md
 *   3. each SKILL.md has a frontmatter name and description
 */

package com.bradflaugher.lfe.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledSkillsTest {
  private val expected = setOf(
    "budget-tracker",
    "calculate-hash",
    "interactive-map",
    "mood-tracker",
    "password-generator",
    "qr-code",
    "query-wikipedia",
    "send-email",
    "translator",
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

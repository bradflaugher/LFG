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

import com.google.gson.annotations.SerializedName

/** A skill in the skill allowlist. */
data class AllowedSkill(
  val name: String,
  val description: String,
  @SerializedName("skillUrl") val skillUrl: String,
  @SerializedName("attributionLabel") val attributionLabel: String? = null,
  @SerializedName("attributionUrl") val attributionUrl: String? = null,
)

/** The skill allowlist. */
data class SkillAllowlist(@SerializedName("featuredSkills") val featuredSkills: List<AllowedSkill>)

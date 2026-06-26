package com.bradflaugher.lfg.data

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
data class SkillAllowlist(
  @SerializedName("featuredSkills") val featuredSkills: List<AllowedSkill>,
)

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

import androidx.annotation.StringRes
import com.bradflaugher.lfe.R

/**
 * Stores basic info about a Category
 *
 * A category is a tab on the home page which contains a list of tasks. Category is set through
 * Task.
 */
data class CategoryInfo(
  // The id of the category.
  val id: String,
  // The string resource id of the label of the resource, for display purpose.
  @StringRes val labelStringRes: Int? = null,
  // The string label. It takes precedence over labelStringRes above.
  val label: String? = null,
)

/** Pre-defined categories. */
object Category {
  val LLM = CategoryInfo(id = "llm", labelStringRes = R.string.category_llm)
  val CLASSICAL_ML = CategoryInfo(id = "classical_ml", labelStringRes = R.string.category_llm)
  val EXPERIMENTAL =
    CategoryInfo(id = "experimental", labelStringRes = R.string.category_experimental)
}

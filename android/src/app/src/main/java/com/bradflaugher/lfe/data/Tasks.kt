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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.bradflaugher.lfe.R

/**
 * LFE has exactly one task — the agent-skills chat — but we keep the Task abstraction so
 * adding more is a one-file change.
 */
data class Task(
  val id: String,
  val label: String,
  val category: CategoryInfo,
  val icon: ImageVector? = null,
  val iconVectorResourceId: Int? = null,
  val description: String,
  val shortDescription: String = "",
  val docUrl: String = "",
  val sourceCodeUrl: String = "",
  val models: MutableList<Model>,
  val modelNames: List<String> = listOf(),
  val handleModelConfigChangesInTask: Boolean = false,
  val experimental: Boolean = false,
  val newFeature: Boolean = false,
  val useThemeColor: Boolean = false,
  val defaultSystemPrompt: String = "",
  @StringRes val agentNameRes: Int = R.string.chat_generic_agent_name,
  @StringRes val textInputPlaceHolderRes: Int = R.string.chat_textinput_placeholder,
  var index: Int = -1,
  val updateTrigger: MutableState<Long> = mutableLongStateOf(0),
) {
  fun allowCapability(
    capability: ModelCapability,
    model: Model,
  ): Boolean {
    return model.capabilityToTaskTypes[capability]?.contains(id) == true
  }
}

object BuiltInTaskId {
  const val LLM_AGENT_CHAT = "llm_agent_chat"
}

fun isLegacyTasks(
  @Suppress("UNUSED_PARAMETER") id: String,
): Boolean = false

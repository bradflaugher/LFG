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
package com.bradflaugher.lfg.common

import com.bradflaugher.lfg.data.SystemPromptRepository
import com.bradflaugher.lfg.data.Task
import kotlinx.coroutines.flow.firstOrNull

/** Helper object for system prompt retrieval and compilation. */
object SystemPromptHelper {
  /**
   * Retrieves the effective system prompt for the given [Task].
   *
   * Returns the user-defined custom prompt from the [SystemPromptRepository] if available;
   * otherwise, falls back to the task's default system prompt.
   *
   * @param repo The optional [SystemPromptRepository] for custom overrides. If null, returns the
   *   default.
   * @param task The target [Task] containing the identifier and the default fallback system prompt.
   * @return A [String] representing the effective system prompt instructions.
   */
  suspend fun getEffectiveSystemPrompt(
    repo: SystemPromptRepository?,
    task: Task,
  ): String {
    if (repo == null) return task.defaultSystemPrompt
    val customPrompt = repo.getCustomSystemPrompt(task.id).firstOrNull()
    return customPrompt ?: task.defaultSystemPrompt
  }
}

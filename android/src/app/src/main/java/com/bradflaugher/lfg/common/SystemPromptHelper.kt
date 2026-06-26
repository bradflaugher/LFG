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

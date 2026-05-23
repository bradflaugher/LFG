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

import androidx.datastore.core.DataStore
import com.bradflaugher.lfe.proto.UserData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Repository for managing custom system prompts per task. */
@Singleton
open class SystemPromptRepository
  @Inject
  constructor(private val userDataDataStore: DataStore<UserData>) {
    private fun getKey(taskId: String): String = "system_prompt_$taskId"

    /**
     * Updates the user-defined custom system prompt for the given [taskId] directly in the DataStore.
     *
     * @param taskId The ID of the task.
     * @param newPrompt The new custom system prompt string.
     */
    suspend fun updateSystemPrompt(
      taskId: String,
      newPrompt: String,
    ) {
      userDataDataStore.updateData { userData ->
        userData.toBuilder().putSecrets(getKey(taskId), newPrompt).build()
      }
    }

    /**
     * Retrieves a Flow of the user-defined custom system prompt for the given [taskId] directly from
     * the DataStore.
     *
     * This method returns ONLY the user's saved prompt, or null if no custom prompt has been set. It
     * does NOT include any fallback to default system prompts.
     *
     * Most call sites should prefer using [SystemPromptHelper.getEffectiveSystemPrompt], which
     * includes the necessary fallback logic. Direct use of this method is rare.
     *
     * @param taskId The ID of the task.
     * @return A Flow emitting the custom system prompt string or null.
     */
    fun getCustomSystemPrompt(taskId: String): Flow<String?> {
      return userDataDataStore.data.map { it.secretsMap[getKey(taskId)] }
    }

    /**
     * Clears the user-defined custom system prompt for the given [taskId] directly from the
     * DataStore.
     *
     * @param taskId The ID of the task.
     */
    suspend fun clearCustomSystemPrompt(taskId: String) {
      userDataDataStore.updateData { userData ->
        if (userData.secretsMap.containsKey(getKey(taskId))) {
          userData.toBuilder().removeSecrets(getKey(taskId)).build()
        } else {
          userData
        }
      }
    }
  }

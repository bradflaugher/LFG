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

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.customtasks.common.CustomTask
import com.bradflaugher.lfe.customtasks.common.CustomTaskDataForBuiltinTask
import com.bradflaugher.lfe.data.BuiltInTaskId
import com.bradflaugher.lfe.data.Category
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.Task
import com.bradflaugher.lfe.proto.Skill
import com.bradflaugher.lfe.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AGAgentChatTask"

// The default system prompt for the agent chat task with only skills.
internal const val DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY =
  """
  You are an AI assistant that helps users by answering questions and completes tasks using skills. For EVERY new task or request or question, you MUST execute the following steps in exact order. You MUST NOT skip any steps.

  CRITICAL RULE: You MUST execute all steps silently. Do NOT generate or output any internal thoughts, reasoning, explanations, or intermediate text at ANY step.

  1. First, find the most relevant skill from the following list:

  ___SKILLS___

  After this step you MUST go to next step. You MUST NOT use `run_intent` under any circumstances at this step.

  2. If a relevant skill exists, use the `load_skill` tool to read its instructions. You MUST NOT use `run_intent` under any circumstances at this step.

  3. Follow the skill's instructions exactly to complete the task. You MUST NOT output any intermediate thoughts or status updates. No exceptions! Output ONLY the final result when successful. It should contain one-sentence summary of the action taken, and the final result of the skill.

  4. If no relevant skill is found, output "No relevant skills found" and stop.
  """

internal val DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED =
  DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY.trimIndent()

class AgentChatTask
  @Inject
  constructor() : CustomTask {
    private val agentTools = AgentTools()

    override val task: Task =
      Task(
        id = BuiltInTaskId.LLM_AGENT_CHAT,
        label = "Agent Skills",
        category = Category.LLM,
        iconVectorResourceId = R.drawable.agent,
        newFeature = true,
        models = mutableListOf(),
        description = "Chat with on-device large language models with skills",
        shortDescription = "Complete agentic tasks with chat",
        docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
        sourceCodeUrl =
          "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/agentchat/",
        textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
        defaultSystemPrompt = DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED,
      )

    override fun initializeModelFn(
      context: Context,
      coroutineScope: CoroutineScope,
      model: Model,
      systemInstruction: Contents?,
      onDone: (String) -> Unit,
    ) {
      val initialSystemPrompt = systemInstruction?.toString() ?: task.defaultSystemPrompt
      coroutineScope.launch(Dispatchers.Default) {
        agentTools.skillManagerViewModel.loadSkills()

        val baseSystemPrompt = getEffectiveBaseSystemPrompt(initialSystemPrompt)

        val finalSystemInstruction =
          injectSkills(
            baseSystemPrompt = baseSystemPrompt,
            skills = agentTools.skillManagerViewModel.getSelectedSkills(),
          )

        LlmChatModelHelper.initialize(
          context = context,
          model = model,
          taskId = task.id,
          supportImage = model.llmSupportImage,
          supportAudio = model.llmSupportAudio,
          onDone = onDone,
          systemInstruction = finalSystemInstruction,
          tools = listOf(tool(agentTools)),
          enableConversationConstrainedDecoding = true,
        )
      }
    }

    override fun cleanUpModelFn(
      context: Context,
      coroutineScope: CoroutineScope,
      model: Model,
      onDone: () -> Unit,
    ) {
      LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
    }

    @Composable
    override fun MainScreen(data: Any) {
      val myData = data as CustomTaskDataForBuiltinTask
      AgentChatScreen(
        task = task,
        modelManagerViewModel = myData.modelManagerViewModel,
        navigateUp = myData.onNavUp,
        onSettingsClicked = myData.onSettingsClicked,
        agentTools = agentTools,
        initialQuery = myData.initialQuery,
      )
    }
  }

@Module
@InstallIn(SingletonComponent::class)
internal object AgentChatTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return AgentChatTask()
  }
}

fun injectSkills(
  baseSystemPrompt: String,
  skills: List<Skill>,
): Contents {
  val selectedSkillsNamesAndDescriptions =
    skills
      .filter { it.selected }
      .joinToString("\n\n") { skill ->
        "- Skill name: \"${skill.name}\"\n- Description: ${skill.description}"
      }

  val systemPrompt =
    if (selectedSkillsNamesAndDescriptions.isBlank()) {
      ""
    } else {
      baseSystemPrompt.replace("___SKILLS___", selectedSkillsNamesAndDescriptions)
    }
  Log.d(TAG, "System prompt:\n$systemPrompt")
  return Contents.of(systemPrompt)
}

// Check whether the system prompt is the default one.
internal fun isDefaultSystemPrompt(prompt: String): Boolean {
  return prompt == DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED
}

// Returns the effective default system prompt.
internal fun getEffectiveBaseSystemPrompt(currentPrompt: String): String {
  return if (isDefaultSystemPrompt(currentPrompt)) {
    DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED
  } else {
    currentPrompt
  }
}

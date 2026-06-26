package com.bradflaugher.lfg.ui.llmchat

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.bradflaugher.lfg.data.BuiltInTaskId
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.ModelCapability
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.chat.ChatMessage
import com.bradflaugher.lfg.ui.common.chat.ChatMessageAudioClip
import com.bradflaugher.lfg.ui.common.chat.ChatMessageImage
import com.bradflaugher.lfg.ui.common.chat.ChatMessageText
import com.bradflaugher.lfg.ui.common.chat.ChatSide
import com.bradflaugher.lfg.ui.common.chat.ChatView
import com.bradflaugher.lfg.ui.common.chat.SendMessageTrigger
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message

private const val TAG = "AGLlmChatScreen"

/**
 * Reusable chat shell used by [AgentChatScreen]. Wraps [ChatView] with the agent's response-
 * generation lifecycle. Kept as a separate composable so the agent screen can layer its own
 * skill orchestration on top.
 */
@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  taskId: String = BuiltInTaskId.LLM_AGENT_CHAT,
  onSettingsClicked: () -> Unit = {},
  onFirstToken: (Model) -> Unit = {},
  onGenerateResponseDone: (Model) -> Unit = {},
  onSkillClicked: () -> Unit = {},
  onResetSessionClickedOverride: ((Task, Model, List<ChatMessage>, Boolean, () -> Unit) -> Unit)? =
    null,
  composableBelowMessageList: @Composable (Model) -> Unit = {},
  viewModel: AgentChatViewModel = hiltViewModel(),
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  emptyStateComposable: @Composable (Model) -> Unit = {},
  sendMessageTrigger: SendMessageTrigger? = null,
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  getActiveSkills: () -> List<String> = { emptyList() },
  skillCount: Int = 0,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = taskId,
    navigateUp = navigateUp,
    onSettingsClicked = onSettingsClicked,
    modifier = modifier,
    onSkillClicked = onSkillClicked,
    onFirstToken = onFirstToken,
    onGenerateResponseDone = onGenerateResponseDone,
    onResetSessionClickedOverride = onResetSessionClickedOverride,
    composableBelowMessageList = composableBelowMessageList,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    skillCount = skillCount,
    onSystemPromptChanged = onSystemPromptChanged,
    emptyStateComposable = emptyStateComposable,
    sendMessageTrigger = sendMessageTrigger,
    showImagePicker = showImagePicker,
    showAudioPicker = showAudioPicker,
    getActiveSkills = getActiveSkills,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: AgentChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  taskId: String,
  navigateUp: () -> Unit,
  onSettingsClicked: () -> Unit = {},
  modifier: Modifier = Modifier,
  onSkillClicked: () -> Unit = {},
  onFirstToken: (Model) -> Unit = {},
  onGenerateResponseDone: (Model) -> Unit = {},
  onResetSessionClickedOverride: ((Task, Model, List<ChatMessage>, Boolean, () -> Unit) -> Unit)? =
    null,
  composableBelowMessageList: @Composable (Model) -> Unit = {},
  emptyStateComposable: @Composable (Model) -> Unit = {},
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  sendMessageTrigger: SendMessageTrigger? = null,
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  getActiveSkills: () -> List<String> = { emptyList() },
  skillCount: Int = 0,
) {
  val context = LocalContext.current
  val task = modelManagerViewModel.getTaskById(id = taskId)!!

  ChatView(
    task = task,
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        when (message) {
          is ChatMessageText -> {
            chatMessageText = message
            text = message.content
          }
          is ChatMessageImage -> images.addAll(message.bitmaps)
          is ChatMessageAudioClip -> audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        if (text.isNotEmpty()) {
          modelManagerViewModel.addTextInputHistory(text)
        }
        viewModel.generateResponse(
          model = model,
          input = text,
          images = images,
          audioMessages = audioMessages,
          onFirstToken = onFirstToken,
          onDone = { onGenerateResponseDone(model) },
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
          allowThinking = task.allowCapability(ModelCapability.LLM_THINKING, model),
        )

        Log.d(
          TAG,
          "generate_action capability=${task.id} active_skills=${getActiveSkills().joinToString(",")}",
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
          allowThinking = task.allowCapability(ModelCapability.LLM_THINKING, model),
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model, chatMessages, clearHistory, onDone ->
      val litertMessages = chatMessages.mapNotNull { convertToLitertMessage(it) }
      if (onResetSessionClickedOverride != null) {
        onResetSessionClickedOverride(task, model, chatMessages, clearHistory, onDone)
      } else {
        viewModel.resetSession(
          task = task,
          model = model,
          systemInstruction = Contents.of(curSystemPrompt),
          supportImage = showImagePicker,
          supportAudio = showAudioPicker,
          initialMessages = litertMessages,
          onDone = onDone,
          clearHistory = clearHistory,
        )
      }
    },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },
    onSkillClicked = onSkillClicked,
    navigateUp = navigateUp,
    onSettingsClicked = onSettingsClicked,
    skillCount = skillCount,
    modifier = modifier,
    composableBelowMessageList = composableBelowMessageList,
    showImagePicker = showImagePicker,
    emptyStateComposable = emptyStateComposable,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = onSystemPromptChanged,
    sendMessageTrigger = sendMessageTrigger,
    showAudioPicker = showAudioPicker,
  )
}

private fun convertToLitertMessage(chatMessage: ChatMessage): Message? {
  // Images and audio aren't replayed into LLM context yet — encoders can stall chat history load.
  if (chatMessage is ChatMessageText) {
    return when (chatMessage.side) {
      ChatSide.USER -> Message.user(chatMessage.content)
      ChatSide.AGENT -> Message.model(chatMessage.content)
      ChatSide.SYSTEM -> null
    }
  }
  return null
}

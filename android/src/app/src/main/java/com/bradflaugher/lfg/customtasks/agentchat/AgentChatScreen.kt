/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.customtasks.agentchat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bradflaugher.lfg.R
import com.bradflaugher.lfg.common.AskInfoAgentAction
import com.bradflaugher.lfg.common.CallJsAgentAction
import com.bradflaugher.lfg.common.ClickAndReadWebpageAgentAction
import com.bradflaugher.lfg.common.FetchArticleAgentAction
import com.bradflaugher.lfg.common.FetchLinksAgentAction
import com.bradflaugher.lfg.common.LOCAL_URL_BASE
import com.bradflaugher.lfg.common.RequestPermissionAgentAction
import com.bradflaugher.lfg.common.SkillProgressAgentAction
import com.bradflaugher.lfg.data.BuiltInTaskId
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.Task
import com.bradflaugher.lfg.ui.common.BaseGalleryWebViewClient
import com.bradflaugher.lfg.ui.common.GalleryWebView
import com.bradflaugher.lfg.ui.common.chat.ChatMessage
import com.bradflaugher.lfg.ui.common.chat.ChatMessageCollapsableProgressPanel
import com.bradflaugher.lfg.ui.common.chat.ChatMessageImage
import com.bradflaugher.lfg.ui.common.chat.ChatMessageText
import com.bradflaugher.lfg.ui.common.chat.ChatMessageType
import com.bradflaugher.lfg.ui.common.chat.ChatMessageWebView
import com.bradflaugher.lfg.ui.common.chat.ChatSide
import com.bradflaugher.lfg.ui.common.chat.LogMessage
import com.bradflaugher.lfg.ui.common.chat.LogMessageLevel
import com.bradflaugher.lfg.ui.common.chat.SendMessageTrigger
import com.bradflaugher.lfg.ui.llmchat.AgentChatViewModel
import com.bradflaugher.lfg.ui.llmchat.LlmChatScreen
import com.bradflaugher.lfg.ui.modelmanager.ModelInitializationStatusType
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.tool
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject

private const val TAG = "AGAgentChatScreen"

@SuppressLint("JavascriptInterface")
@Composable
fun AgentChatScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  agentTools: AgentTools,
  onSettingsClicked: () -> Unit = {},
  viewModel: AgentChatViewModel = hiltViewModel(),
  skillManagerViewModel: SkillManagerViewModel = hiltViewModel(),
  initialQuery: String? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val chatViewJavascriptInterface: ChatWebViewJavascriptInterface = remember { ChatWebViewJavascriptInterface() }
  agentTools.context = context
  agentTools.skillManagerViewModel = skillManagerViewModel
  agentTools.taskId = task.id

  val density = LocalDensity.current
  val windowInfo = LocalWindowInfo.current
  val screenWidthDp = remember { with(density) { windowInfo.containerSize.width.toDp() } }

  var showSkillManagerBottomSheet by remember { mutableStateOf(false) }
  var showAskInfoDialog by remember { mutableStateOf(false) }
  var currentAskInfoAction by remember { mutableStateOf<AskInfoAgentAction?>(null) }
  var askInfoInputValue by remember { mutableStateOf("") }
  var webViewRef: WebView? by remember { mutableStateOf(null) }
  val chatWebViewClient = remember { ChatWebViewClient(context = context) }
  var curSystemPrompt by remember { mutableStateOf(task.defaultSystemPrompt) }
  val systemPromptUpdatedMessage = stringResource(R.string.system_prompt_updated)
  var sendMessageTrigger by remember { mutableStateOf<SendMessageTrigger?>(null) }
  var showAlertForDisabledSkill by remember { mutableStateOf(false) }
  var disabledSkillName by remember { mutableStateOf("") }

  var currentPermissionAction by remember { mutableStateOf<RequestPermissionAgentAction?>(null) }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      currentPermissionAction?.result?.complete(granted)
      currentPermissionAction = null
    }

  LaunchedEffect(task) { viewModel.loadSystemPrompt(task) }
  val uiSystemPrompt by viewModel.uiSystemPrompt.collectAsState()

  val llmChatUiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val skillUiState by skillManagerViewModel.uiState.collectAsState()

  val skillCount = skillUiState.skills.count { it.skill.selected }

  LaunchedEffect(uiSystemPrompt) {
    curSystemPrompt = getEffectiveBaseSystemPrompt(uiSystemPrompt)
  }

  val selectedModel = modelManagerUiState.selectedModel
  val modelInitStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]

  var initialQueryConsumed by remember { mutableStateOf(false) }

  LaunchedEffect(
    llmChatUiState.isResettingSession,
    modelInitStatus?.status,
    selectedModel.name,
    initialQuery,
  ) {
    if (
      !initialQuery.isNullOrEmpty() &&
      !initialQueryConsumed &&
      modelInitStatus?.status == ModelInitializationStatusType.INITIALIZED &&
      !llmChatUiState.isResettingSession
    ) {
      initialQueryConsumed = true
      sendMessageTrigger =
        SendMessageTrigger(
          model = selectedModel,
          messages = listOf(ChatMessageText(content = initialQuery, side = ChatSide.USER)),
        )
    }
  }

  LlmChatScreen(
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_AGENT_CHAT,
    navigateUp = navigateUp,
    onSettingsClicked = onSettingsClicked,
    skillCount = skillCount,
    onFirstToken = { model ->
      scope.launch(Dispatchers.Main) {
        updateProgressPanel(viewModel = viewModel, model = model, agentTools = agentTools)
      }
    },
    onGenerateResponseDone = { model ->
      scope.launch(Dispatchers.Main) {
        agentTools.resultImageToShow?.let { resultImage ->
          resultImage.base64?.let { base64 ->
            decodeBase64ToBitmap(base64String = base64)?.let { bitmap ->
              viewModel.addMessage(
                model = model,
                message =
                  ChatMessageImage(
                    bitmaps = listOf(bitmap),
                    imageBitMaps = listOf(bitmap.asImageBitmap()),
                    side = ChatSide.AGENT,
                    maxSize = (screenWidthDp.value * 0.8).toInt(),
                    latencyMs = -1.0f,
                    hideSenderLabel = true,
                  ),
              )
            }
          }
          agentTools.resultImageToShow = null
        }

        agentTools.resultWebviewToShow?.let { webview ->
          val url = webview.url ?: ""
          val iframe = webview.iframe == true
          val aspectRatio = webview.aspectRatio ?: 1.333f
          viewModel.addMessage(
            model = model,
            message =
              ChatMessageWebView(
                url = url,
                iframe = iframe,
                aspectRatio = aspectRatio,
                hideSenderLabel = true,
              ),
          )
          agentTools.resultWebviewToShow = null
        }
        updateProgressPanel(viewModel = viewModel, model = model, agentTools = agentTools)
      }
    },
    onResetSessionClickedOverride = { task, _, initialMessages, clearHistory, onDone ->
      resetSessionWithCurrentSkills(
        viewModel,
        modelManagerViewModel,
        skillManagerViewModel,
        task,
        curSystemPrompt,
        agentTools,
        onDone = { onDone() },
        initialMessages = initialMessages,
        clearHistory = clearHistory,
      )
    },
    onSkillClicked = { showSkillManagerBottomSheet = true },
    showImagePicker = true,
    showAudioPicker = true,
    getActiveSkills = {
      skillManagerViewModel.getSelectedSkills().map { skill ->
        skillManagerViewModel.getSkillShortId(skill)
      }
    },
    composableBelowMessageList = { model ->
      val actionChannel = agentTools.actionChannel
      val doneIcon = ImageVector.vectorResource(R.drawable.skill)
      val currentModel by rememberUpdatedState(model)
      LaunchedEffect(actionChannel) {
        for (action in actionChannel) {
          Log.d(TAG, "Handling action: " + action.toString())
          when (action) {
            is SkillProgressAgentAction -> {
              viewModel.updateCollapsableProgressPanelMessage(
                model = currentModel,
                title = action.label,
                inProgress = action.inProgress,
                doneIcon = doneIcon,
                addItemTitle = action.addItemTitle,
                addItemDescription = action.addItemDescription,
                customData = action.customData,
              )
            }
            is CallJsAgentAction -> {
              val skillName =
                when {
                  action.url.contains("/skills/") ->
                    action.url.substringAfter("/skills/").substringBefore("/")
                  action.url.startsWith("$LOCAL_URL_BASE/") ->
                    action.url.substringAfter("$LOCAL_URL_BASE/").substringBefore("/")
                  else -> action.url
                }
              try {
                // Safety net timeout — never hang the chat or tool execution.
                val timeoutJob = launch {
                  delay(60_000L)
                  if (!action.result.isCompleted) {
                    Log.e(TAG, "JS execution timed out for skill " + skillName)
                    action.result.complete(
                      "{\"error\": \"Skill execution timed out. Check network connection.\"}",
                    )
                  }
                }

                action.result.invokeOnCompletion {
                  timeoutJob.cancel()
                  chatViewJavascriptInterface.onResultListener = null
                }

                suspendCancellableCoroutine<Unit> { continuation ->
                  chatWebViewClient.setPageLoadListener {
                    chatWebViewClient.setPageLoadListener(null)
                    continuation.resume(Unit)
                  }
                  Log.d(TAG, "Loading url: " + action.url)
                  webViewRef?.loadUrl(action.url)
                }

                chatViewJavascriptInterface.onResultListener = { result ->
                  Log.d(TAG, "Got result: " + result)
                  action.result.complete(result)
                }

                val safeData = JSONObject.quote(action.data)
                val safeSecret = JSONObject.quote(action.secret)
                val script =
                  """
                  (async function() {
                      var startTs = Date.now();
                      while(true) {
                        if (typeof ai_edge_gallery_get_result === 'function') {
                          break;
                        }
                        await new Promise(resolve=>{
                          setTimeout(resolve, 100)
                        });
                        if (Date.now() - startTs > 10000) {
                          break;
                        }
                      }
                      if (typeof ai_edge_gallery_get_result === 'function') {
                        var result = await ai_edge_gallery_get_result($safeData, $safeSecret);
                        AiEdgeGallery.onResultReady(result);
                      } else {
                        AiEdgeGallery.onResultReady(JSON.stringify({error: "Skill interface 'ai_edge_gallery_get_result' not defined in script."}));
                      }
                  })()
                  """
                    .trimIndent()
                webViewRef?.evaluateJavascript(script, null)
              } catch (e: Exception) {
                Log.e(TAG, "Skill " + skillName + " execution failed", e)
                action.result.completeExceptionally(e)
              }
            }
            is AskInfoAgentAction -> {
              currentAskInfoAction = action
              askInfoInputValue = ""
              showAskInfoDialog = true
            }
            is RequestPermissionAgentAction -> {
              currentPermissionAction = action
              permissionLauncher.launch(action.permission)
            }
            is FetchArticleAgentAction -> {
              try {
                val result = ArticleFetcher.fetch(context, action.url)
                action.result.complete(result)
              } catch (e: Exception) {
                action.result.complete(ArticleFetcher.errorJson(e.message ?: "fetch failed"))
              }
            }
            is FetchLinksAgentAction -> {
              try {
                val result = ArticleFetcher.fetchLinks(context, action.url)
                action.result.complete(result)
              } catch (e: Exception) {
                action.result.complete(ArticleFetcher.errorJson(e.message ?: "fetch failed"))
              }
            }
            is ClickAndReadWebpageAgentAction -> {
              try {
                val result = ArticleFetcher.clickAndFetch(context, action.url, action.selector)
                action.result.complete(result)
              } catch (e: Exception) {
                action.result.complete(ArticleFetcher.errorJson(e.message ?: "click and fetch failed"))
              }
            }
          }
        }
      }

      GalleryWebView(
        modifier = Modifier.size(300.dp),
        onWebViewCreated = { webView ->
          webViewRef = webView
          webView.addJavascriptInterface(chatViewJavascriptInterface, "AiEdgeGallery")
        },
        customWebViewClient = chatWebViewClient,
        onConsoleMessage = { consoleMessage ->
          consoleMessage?.let { curConsoleMessage ->
            val logMessage =
              LogMessage(
                level =
                  when (curConsoleMessage.messageLevel()) {
                    ConsoleMessage.MessageLevel.LOG -> LogMessageLevel.Info
                    ConsoleMessage.MessageLevel.ERROR -> LogMessageLevel.Error
                    ConsoleMessage.MessageLevel.WARNING -> LogMessageLevel.Warning
                    else -> LogMessageLevel.Info
                  },
                source = curConsoleMessage.sourceId(),
                lineNumber = curConsoleMessage.lineNumber(),
                message = curConsoleMessage.message(),
              )
            viewModel.addLogMessageToLastCollapsableProgressPanel(
              model = model,
              logMessage = logMessage,
            )
            Log.d(
              TAG,
              curConsoleMessage.message() +
                " -- From line " +
                curConsoleMessage.lineNumber() +
                " of " +
                curConsoleMessage.sourceId(),
            )
          }
        },
      )
    },
    allowEditingSystemPrompt = true,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = { newPrompt ->
      curSystemPrompt = newPrompt
      viewModel.applySystemPromptChange(
        task = task,
        model = modelManagerViewModel.uiState.value.selectedModel,
        newPrompt = newPrompt,
        systemPromptUpdatedMessage = systemPromptUpdatedMessage,
      )
    },
    emptyStateComposable = { _ -> },
    sendMessageTrigger = sendMessageTrigger,
  )

  if (showAskInfoDialog && currentAskInfoAction != null) {
    val action = currentAskInfoAction!!
    SecretEditorDialog(
      title = action.dialogTitle,
      fieldLabel = action.fieldLabel,
      value = askInfoInputValue,
      onValueChange = { askInfoInputValue = it },
      onDone = {
        action.result.complete(askInfoInputValue)
        showAskInfoDialog = false
        currentAskInfoAction = null
      },
      onDismiss = {
        action.result.complete("")
        showAskInfoDialog = false
        currentAskInfoAction = null
      },
    )
  }

  if (showSkillManagerBottomSheet) {
    SkillManagerBottomSheet(
      agentTools = agentTools,
      skillManagerViewModel = skillManagerViewModel,
      onDismiss = { selectedSkillsChanged ->
        showSkillManagerBottomSheet = false
        if (selectedSkillsChanged) {
          Log.d(TAG, "Selected skill changed. Resetting conversation.")
          resetSessionWithCurrentSkills(
            viewModel,
            modelManagerViewModel,
            skillManagerViewModel,
            task,
            curSystemPrompt,
            agentTools,
          )
        }
      },
    )
  }

  if (showAlertForDisabledSkill) {
    AlertDialog(
      onDismissRequest = { showAlertForDisabledSkill = false },
      title = { Text("The \"" + disabledSkillName + "\" skill is currently disabled") },
      text = { Text(stringResource(R.string.enable_skill_dialog_content)) },
      confirmButton = {
        Button(onClick = { showAlertForDisabledSkill = false }) {
          Text(stringResource(R.string.ok))
        }
      },
    )
  }
}

private fun updateProgressPanel(
  viewModel: AgentChatViewModel,
  model: Model,
  agentTools: AgentTools,
) {
  val lastProgressPanelMessage =
    viewModel.getLastMessageWithType(
      model = model,
      type = ChatMessageType.COLLAPSABLE_PROGRESS_PANEL,
    )
  if (
    lastProgressPanelMessage != null &&
    lastProgressPanelMessage is ChatMessageCollapsableProgressPanel
  ) {
    val title = lastProgressPanelMessage.title
    val transformed =
      when {
        title.startsWith("Loading") -> title.replace("Loading", "Loaded")
        title.startsWith("Calling") -> title.replace("Calling", "Called")
        title.startsWith("Executing") -> title.replace("Executing", "Executed")
        else -> title
      }
    agentTools.sendAgentAction(
      SkillProgressAgentAction(label = transformed, inProgress = false),
    )
  }
}

private fun resetSessionWithCurrentSkills(
  viewModel: AgentChatViewModel,
  modelManagerViewModel: ModelManagerViewModel,
  skillManagerViewModel: SkillManagerViewModel,
  task: Task,
  curSystemPrompt: String,
  agentTools: AgentTools,
  onDone: (Model) -> Unit = {},
  initialMessages: List<ChatMessage> = listOf(),
  clearHistory: Boolean = true,
) {
  val model = modelManagerViewModel.uiState.value.selectedModel
  val litertMessages =
    initialMessages.mapNotNull { chatMessage ->
      if (chatMessage is ChatMessageText) {
        if (chatMessage.side == ChatSide.USER) {
          Message.user(chatMessage.content)
        } else {
          Message.model(chatMessage.content)
        }
      } else {
        null
      }
    }
  val actualSystemPrompt = getEffectiveBaseSystemPrompt(curSystemPrompt)
  viewModel.resetSession(
    task = task,
    model = model,
    systemInstruction =
      injectSkills(
        baseSystemPrompt = actualSystemPrompt,
        skills = skillManagerViewModel.getSelectedSkills(),
      ),
    tools = listOf(tool(agentTools)),
    supportImage = model.llmSupportImage,
    supportAudio = model.llmSupportAudio,
    onDone = { onDone(model) },
    enableConversationConstrainedDecoding = true,
    initialMessages = litertMessages,
    clearHistory = clearHistory,
  )
}

class ChatWebViewJavascriptInterface {
  var onResultListener: ((String) -> Unit)? = null

  @JavascriptInterface
  fun onResultReady(result: String) {
    onResultListener?.invoke(result)
  }
}

class ChatWebViewClient(val context: Context) : BaseGalleryWebViewClient(context = context) {
  private var onPageLoaded: (() -> Unit)? = null

  fun setPageLoadListener(listener: (() -> Unit)?) {
    onPageLoaded = listener
  }

  override fun onPageFinished(
    view: WebView?,
    url: String?,
  ) {
    super.onPageFinished(view, url)
    Log.d(TAG, "page loaded")
    onPageLoaded?.invoke()
  }
}

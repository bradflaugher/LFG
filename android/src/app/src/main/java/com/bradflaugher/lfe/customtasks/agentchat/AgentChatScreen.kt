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
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.common.AskInfoAgentAction
import com.bradflaugher.lfe.common.CallJsAgentAction
import com.bradflaugher.lfe.common.LOCAL_URL_BASE
import com.bradflaugher.lfe.common.RequestPermissionAgentAction
import com.bradflaugher.lfe.common.SkillProgressAgentAction
import com.bradflaugher.lfe.data.AgentSkillsURLs
import com.bradflaugher.lfe.data.BuiltInTaskId
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.Task
import com.bradflaugher.lfe.ui.common.BaseGalleryWebViewClient
import com.bradflaugher.lfe.ui.common.GalleryWebView
import com.bradflaugher.lfe.ui.common.buildTrackableUrlAnnotatedString
import com.bradflaugher.lfe.ui.common.chat.ChatMessage
import com.bradflaugher.lfe.ui.common.chat.ChatMessageCollapsableProgressPanel
import com.bradflaugher.lfe.ui.common.chat.ChatMessageImage
import com.bradflaugher.lfe.ui.common.chat.ChatMessageText
import com.bradflaugher.lfe.ui.common.chat.ChatMessageType
import com.bradflaugher.lfe.ui.common.chat.ChatMessageWebView
import com.bradflaugher.lfe.ui.common.chat.ChatSide
import com.bradflaugher.lfe.ui.common.chat.LogMessage
import com.bradflaugher.lfe.ui.common.chat.LogMessageLevel
import com.bradflaugher.lfe.ui.common.chat.SendMessageTrigger
import com.bradflaugher.lfe.ui.llmchat.AgentChatViewModel
import com.bradflaugher.lfe.ui.llmchat.LlmChatScreen
import com.bradflaugher.lfe.ui.modelmanager.ModelInitializationStatusType
import com.bradflaugher.lfe.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.tool
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject

private const val TAG = "AGAgentChatScreen"
private val chatViewJavascriptInterface = ChatWebViewJavascriptInterface()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  agentTools: AgentTools,
  viewModel: AgentChatViewModel = hiltViewModel(),
  skillManagerViewModel: SkillManagerViewModel = hiltViewModel(),
  initialQuery: String? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
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
          Log.d(TAG, "Handling action: $action")
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
                launch {
                  delay(60_000L)
                  if (!action.result.isCompleted) {
                    Log.e(TAG, "JS execution timed out for skill $skillName")
                    action.result.complete(
                      "{\"error\": \"Skill execution timed out. Check network connection.\"}",
                    )
                  }
                }

                suspendCancellableCoroutine<Unit> { continuation ->
                  chatWebViewClient.setPageLoadListener {
                    chatWebViewClient.setPageLoadListener(null)
                    continuation.resume(Unit)
                  }
                  Log.d(TAG, "Loading url: ${action.url}")
                  webViewRef?.loadUrl(action.url)
                }

                chatViewJavascriptInterface.onResultListener = { result ->
                  Log.d(TAG, "Got result: $result")
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
                      var result = await ai_edge_gallery_get_result($safeData, $safeSecret);
                      AiEdgeGallery.onResultReady(result);
                  })()
                  """
                    .trimIndent()
                webViewRef?.evaluateJavascript(script, null)
              } catch (e: Exception) {
                Log.e(TAG, "Skill $skillName execution failed", e)
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
              "${curConsoleMessage.message()} " +
                "-- From line ${curConsoleMessage.lineNumber()} of ${curConsoleMessage.sourceId()}",
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
    emptyStateComposable = { model ->
      val uiState by viewModel.uiState.collectAsState()
      val mmState by modelManagerViewModel.uiState.collectAsState()
      val modelInitializationStatus = mmState.modelInitializationStatus[model.name]
      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
          !WindowInsets.isImeVisible,
          enter = fadeIn(animationSpec = tween(200)),
          exit = fadeOut(animationSpec = tween(200)),
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              modifier =
                Modifier.align(Alignment.Center)
                  .padding(horizontal = 48.dp)
                  .padding(bottom = 48.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                stringResource(R.string.introducing),
                style = MaterialTheme.typography.headlineSmall,
              )
              Text(
                stringResource(R.string.agent_skills),
                style =
                  MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
                    brush =
                      Brush.linearGradient(colors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1))),
                  ),
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
              )
              Text(
                buildAnnotatedString {
                  append("Use specialized, high-order reasoning by loading different skills or ")
                  append(
                    buildTrackableUrlAnnotatedString(
                      url = AgentSkillsURLs.REPOSITORY,
                      linkText = "creating your own",
                    ),
                  )
                  append(". Explore community contributed skills on ")
                  append(
                    buildTrackableUrlAnnotatedString(
                      url = AgentSkillsURLs.DISCUSSIONS,
                      linkText = "GitHub discussions",
                    ),
                  )
                  append(".\n\nTry tapping a sample prompt below to see Agent Skills in action!")
                },
                style =
                  MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        Row(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          for (promptChip in TRYOUT_CHIPS) {
            FilledTonalButton(
              enabled =
                modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZED &&
                  !uiState.isResettingSession,
              onClick = {
                if (skillManagerViewModel.isSkillSelected(promptChip.skillName)) {
                  sendMessageTrigger =
                    SendMessageTrigger(
                      model = model,
                      messages =
                        listOf(ChatMessageText(content = promptChip.prompt, side = ChatSide.USER)),
                    )
                } else {
                  disabledSkillName = promptChip.skillName
                  showAlertForDisabledSkill = true
                }
              },
              contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
              Icon(promptChip.icon, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(promptChip.label)
            }
          }
        }
      }
    },
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
      title = { Text("The \"$disabledSkillName\" skill is currently disabled") },
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
    supportImage = true,
    supportAudio = true,
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

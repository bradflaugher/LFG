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
package com.bradflaugher.lfe.ui.common.chat

// import com.bradflaugher.lfe.ui.preview.PreviewChatModel
// import com.bradflaugher.lfe.ui.preview.PreviewModelManagerViewModel
// import com.bradflaugher.lfe.ui.preview.TASK_TEST1
// import com.bradflaugher.lfe.ui.theme.LfeTheme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bradflaugher.lfe.R
import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.data.ModelDownloadStatusType
import com.bradflaugher.lfe.data.Task
import com.bradflaugher.lfe.ui.common.ModelPageAppBar
import com.bradflaugher.lfe.ui.common.copyBitmapToClipboard
import com.bradflaugher.lfe.ui.common.saveBitmapToMediaStore
import com.bradflaugher.lfe.ui.common.shareBitmap
import com.bradflaugher.lfe.ui.modelmanager.ModelInitializationStatusType
import com.bradflaugher.lfe.ui.modelmanager.ModelManagerViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AGChatView"

data class SendMessageTrigger(val model: Model, val messages: List<ChatMessage>)

/**
 * A composable that displays a chat interface, allowing users to interact with different models
 * associated with a given task.
 *
 * This composable provides a horizontal pager for switching between models, a model selector for
 * configuring the selected model, and a chat panel for sending and receiving messages. It also
 * manages model initialization, cleanup, and download status, and handles navigation and system
 * back gestures.
 */
@Composable
fun ChatView(
  task: Task,
  viewModel: ChatViewModel,
  modelManagerViewModel: ModelManagerViewModel,
  onSendMessage: (Model, List<ChatMessage>) -> Unit,
  onRunAgainClicked: (Model, ChatMessage) -> Unit,
  onBenchmarkClicked: (Model, ChatMessage, Int, Int) -> Unit,
  navigateUp: () -> Unit,
  onSettingsClicked: () -> Unit = {},
  modifier: Modifier = Modifier,
  skillCount: Int = 0,
  onResetSessionClicked: (
    model: Model,
    initialMessages: List<ChatMessage>,
    clearHistory: Boolean,
    onDone: () -> Unit,
  ) -> Unit =
    { _, _, _, onDone ->
      onDone()
    },
  onStreamImageMessage: (Model, ChatMessageImage) -> Unit = { _, _ -> },
  onStopButtonClicked: (Model) -> Unit = {},
  onSkillClicked: () -> Unit = {},
  showStopButtonInInputWhenInProgress: Boolean = false,
  composableBelowMessageList: @Composable (Model) -> Unit = {},
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  emptyStateComposable: @Composable (Model) -> Unit = {},
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  sendMessageTrigger: SendMessageTrigger? = null,
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel

  // Image viewer related.
  var selectedImageIndex by remember { mutableIntStateOf(-1) }
  var allImageViewerImages by remember { mutableStateOf<List<Bitmap>>(listOf()) }
  var showImageViewer by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Chat history drawer.
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val allHistorySessions by viewModel.historySessions.collectAsState()
  val historySessions =
    remember(allHistorySessions, task.id) { allHistorySessions.filter { it.taskId == task.id } }

  val context = LocalContext.current

  val currentMessages = uiState.messagesByModel[selectedModel.name] ?: emptyList()
  LaunchedEffect(uiState.inProgress) {
    if (!uiState.inProgress && currentMessages.isNotEmpty()) {
      viewModel.saveSession(
        sessionId = viewModel.currentSessionId,
        messages = currentMessages,
        originalModel = selectedModel.name,
        taskId = task.id,
        context = context,
      )
    }
  }
  val scope = rememberCoroutineScope()
  var navigatingUp by remember { mutableStateOf(false) }

  val handleNavigateUp = {
    navigatingUp = true
    navigateUp()

    // clean up all models.
    scope.launch(Dispatchers.Default) {
      for (model in task.models) {
        modelManagerViewModel.cleanupModel(context = context, task = task, model = model)
      }
    }
  }

  // Initialize model when model/download state changes.
  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
  LaunchedEffect(curDownloadStatus, selectedModel.name) {
    if (!navigatingUp) {
      if (curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED) {
        Log.d(TAG, "Initializing model '${selectedModel.name}' from ChatView launched effect")
        modelManagerViewModel.initializeModel(context, task = task, model = selectedModel)
      }
    }
  }

  LaunchedEffect(sendMessageTrigger) {
    sendMessageTrigger?.let { trigger -> onSendMessage(trigger.model, trigger.messages) }
  }

  // Handle system's edge swipe: close drawer if open, otherwise let the system handle back
  // (chat is the home screen, so the system default is to exit the app).
  BackHandler(enabled = drawerState.isOpen) {
    scope.launch { drawerState.close() }
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          ModalDrawerSheet {
            ChatHistorySideSheetContent(
              history = historySessions,
              onHistoryItemClicked = { sessionId ->
                val session = historySessions.firstOrNull { it.sessionId == sessionId }
                if (session != null) {
                  scope.launch {
                    viewModel.setIsResettingSession(true)
                    val messages =
                      withContext(Dispatchers.IO) { deserializeProtoMessages(session.messagesList) }
                    viewModel.clearAllMessages(selectedModel)
                    for (msg in messages) {
                      viewModel.addMessage(selectedModel, msg)
                    }
                    onResetSessionClicked(selectedModel, messages, /* clearHistory= */ false) {
                      viewModel.setIsResettingSession(false)
                    }
                    viewModel.currentSessionId = session.sessionId
                  }
                }
                scope.launch { drawerState.close() }
              },
              onHistoryItemDeleted = { sessionId ->
                viewModel.deleteSession(sessionId, context)
                if (sessionId == viewModel.currentSessionId) {
                  onResetSessionClicked(selectedModel, emptyList(), /* clearHistory= */ true) {}
                  viewModel.currentSessionId = UUID.randomUUID().toString()
                }
              },
              onHistoryItemsDeleteAll = {
                viewModel.clearAllSessions(context)
                onResetSessionClicked(selectedModel, emptyList(), /* clearHistory= */ true) {}
                viewModel.currentSessionId = UUID.randomUUID().toString()
                scope.launch { drawerState.close() }
              },
              onNewChatClicked = {
                onResetSessionClicked(selectedModel, emptyList(), /* clearHistory= */ true) {}
                viewModel.currentSessionId = UUID.randomUUID().toString()
                scope.launch { drawerState.close() }
              },
              onDismissed = { scope.launch { drawerState.close() } },
            )
          }
        }
      },
      gesturesEnabled = drawerState.isOpen,
    ) {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
          modifier = modifier,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            ModelPageAppBar(
              task = task,
              model = selectedModel,
              modelManagerViewModel = modelManagerViewModel,
              inProgress = uiState.inProgress,
              modelPreparing = uiState.preparing,
              shouldShowHistoryButton = true,
              onConfigChanged = { _, _ -> },
              onSettingsClicked = onSettingsClicked,
              hideBackButton = true,
              onBackClicked = { handleNavigateUp() },
              onModelSelected = { prevModel, curModel ->
                if (prevModel.name != curModel.name) {
                  modelManagerViewModel.cleanupModel(
                    context = context,
                    task = task,
                    model = prevModel,
                  )
                }
                modelManagerViewModel.selectModel(model = curModel)
              },
              allowEditingSystemPrompt = allowEditingSystemPrompt,
              curSystemPrompt = curSystemPrompt,
              onSystemPromptChanged = onSystemPromptChanged,
              onHistoryClicked = { scope.launch { drawerState.open() } },
            )
          },
        ) { innerPadding ->
          Box {
            val curModelDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]

            composableBelowMessageList(selectedModel)

            Column(
              modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            ) {
              AnimatedContent(
                targetState = curModelDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED,
              ) { targetState ->
                when (targetState) {
                  // Main UI when model is downloaded.
                  true ->
                    ChatPanel(
                      modelManagerViewModel = modelManagerViewModel,
                      task = task,
                      selectedModel = selectedModel,
                      viewModel = viewModel,
                      innerPadding = innerPadding,
                      skillCount = skillCount,
                      navigateUp = navigateUp,
                      onSendMessage = { model, messages -> onSendMessage(model, messages) },
                      onRunAgainClicked = onRunAgainClicked,
                      onBenchmarkClicked = onBenchmarkClicked,
                      onStreamImageMessage = onStreamImageMessage,
                      onStreamEnd = { averageFps ->
                        viewModel.addMessage(
                          model = selectedModel,
                          message =
                            ChatMessageInfo(
                              content = "Live camera session ended. Average FPS: $averageFps",
                            ),
                        )
                      },
                      onStopButtonClicked = { onStopButtonClicked(selectedModel) },
                      onImageSelected = { bitmaps, selectedBitmapIndex ->
                        selectedImageIndex = selectedBitmapIndex
                        allImageViewerImages = bitmaps
                        showImageViewer = true
                      },
                      onSkillClicked = onSkillClicked,
                      modifier = Modifier.weight(1f),
                      showStopButtonInInputWhenInProgress = showStopButtonInInputWhenInProgress,
                      showImagePicker = showImagePicker,
                      showAudioPicker = showAudioPicker,
                      emptyStateComposable = emptyStateComposable,
                    )
                  // Model download
                  false ->
                    ModelDownloadStatusInfoPanel(
                      model = selectedModel,
                      task = task,
                      modelManagerViewModel = modelManagerViewModel,
                    )
                }
              }
            }

            // Image viewer.
            if (showImageViewer) {
              Dialog(
                onDismissRequest = { showImageViewer = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
              ) {
                val dialogSnackbarHostState = remember { SnackbarHostState() }
                val pagerState =
                  rememberPagerState(
                    pageCount = { allImageViewerImages.size },
                    initialPage = selectedImageIndex,
                  )
                val scrollEnabled = remember { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                  HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = scrollEnabled.value,
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)),
                  ) { page ->
                    allImageViewerImages[page].let { image ->
                      ZoomableImage(
                        bitmap = image.asImageBitmap(),
                        pagerState = pagerState,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }
                  }

                  val curBitmap = allImageViewerImages.getOrNull(pagerState.currentPage)

                  // Top item: ArrowBack (top left).
                  Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    IconButton(onClick = { showImageViewer = false }) {
                      Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                      )
                    }
                  }

                  // Bottom items: Share, Copy, Save.
                  val copySuccessMsg = stringResource(R.string.snackbar_copy_to_clipboard_success)
                  val saveSuccessMsg = stringResource(R.string.snackbar_save_to_album_success)
                  val saveFailedMsg = stringResource(R.string.snackbar_save_to_album_failed)
                  Row(
                    modifier =
                      Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                  ) {
                    // Share button
                    IconButton(
                      onClick = {
                        curBitmap?.let { bitmap -> scope.launch { context.shareBitmap(bitmap) } }
                      },
                      modifier = Modifier.size(64.dp),
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          Icons.Rounded.Share,
                          contentDescription = stringResource(R.string.share),
                          tint = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = stringResource(R.string.share),
                          color = Color.White,
                          fontSize = 12.sp,
                          textAlign = TextAlign.Center,
                        )
                      }
                    }

                    // Copy button
                    IconButton(
                      onClick = {
                        curBitmap?.let { bitmap ->
                          scope.launch {
                            context.copyBitmapToClipboard(bitmap)
                            dialogSnackbarHostState.showSnackbar(copySuccessMsg)
                          }
                        }
                      },
                      modifier = Modifier.size(64.dp),
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          Icons.Rounded.ContentCopy,
                          contentDescription = stringResource(R.string.copy),
                          tint = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = stringResource(R.string.copy),
                          color = Color.White,
                          fontSize = 12.sp,
                          textAlign = TextAlign.Center,
                        )
                      }
                    }

                    // Save button
                    IconButton(
                      onClick = {
                        curBitmap?.let { bitmap ->
                          scope.launch {
                            val success =
                              context.saveBitmapToMediaStore(
                                bitmap,
                                "chat_image_${System.currentTimeMillis()}.png",
                              )
                            if (success) {
                              dialogSnackbarHostState.showSnackbar(saveSuccessMsg)
                            } else {
                              dialogSnackbarHostState.showSnackbar(saveFailedMsg)
                            }
                          }
                        }
                      },
                      modifier = Modifier.size(64.dp),
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          Icons.Rounded.Download,
                          contentDescription = stringResource(R.string.save),
                          tint = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = stringResource(R.string.save),
                          color = Color.White,
                          fontSize = 12.sp,
                          textAlign = TextAlign.Center,
                        )
                      }
                    }
                  }
                  SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Helper function to construct the first message when a session is restored from history.
 *
 * It prepends the entire text chat history (from User and Model) as context for the message,
 * ensuring the model understands the prior conversation when running the newly restored session.
 *
 * @param history The list of past messages for the selected model.
 * @param originalShortMessage The newly entered message to be added to the history.
 * @return A new [ChatMessageText] with history prepended, or null if there is no valid history.
 */
private fun buildFirstMessageWithHistory(
  history: List<ChatMessage>,
  originalShortMessage: ChatMessageText,
): ChatMessageText? {
  val prefix =
    history
      .mapNotNull {
        when (it) {
          is ChatMessageText ->
            if (it.side == ChatSide.USER) "User:\n${it.content}" else "Model:\n${it.content}"
          else -> null
        }
      }
      .joinToString("\n\n")

  if (prefix.isEmpty()) {
    return null
  }

  return ChatMessageText(
    content = "$prefix\n\nUser:\n${originalShortMessage.content}",
    side = originalShortMessage.side,
    latencyMs = originalShortMessage.latencyMs,
    isMarkdown = originalShortMessage.isMarkdown,
    accelerator = originalShortMessage.accelerator,
    hideSenderLabel = originalShortMessage.hideSenderLabel,
    data = originalShortMessage.data,
  )
}

/**
 * Deserializes a list of [com.bradflaugher.lfe.proto.ChatMessageProto] from persistent
 * storage into the corresponding [ChatMessage] UI models.
 *
 * @param protoMessages The list of saved protobuf messages.
 * @return The list of restored UI/domain message objects.
 */
private fun deserializeProtoMessages(protoMessages: List<com.bradflaugher.lfe.proto.ChatMessageProto>): List<ChatMessage> {
  return protoMessages.mapNotNull { protoMsg ->
    val side =
      when (protoMsg.side) {
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_USER -> ChatSide.USER
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_MODEL -> ChatSide.AGENT
        com.bradflaugher.lfe.proto.ChatSideProto.CHAT_SIDE_SYSTEM -> ChatSide.SYSTEM
        else -> ChatSide.SYSTEM
      }

    when (protoMsg.messageType) {
      "TEXT" ->
        ChatMessageText(
          content = protoMsg.content,
          side = side,
          latencyMs = protoMsg.latencyMs,
          isMarkdown = protoMsg.isMarkdown,
          accelerator = protoMsg.accelerator,
          hideSenderLabel = protoMsg.hideSenderLabel,
        )
      "THINKING" ->
        ChatMessageThinking(
          content = protoMsg.content,
          side = side,
          inProgress = protoMsg.inProgress,
          accelerator = protoMsg.accelerator,
          hideSenderLabel = protoMsg.hideSenderLabel,
        )
      "INFO" -> ChatMessageInfo(protoMsg.content)
      "WARNING" -> ChatMessageWarning(protoMsg.content)
      "ERROR" -> ChatMessageError(protoMsg.content)
      "IMAGE" -> {
        val bitmaps =
          protoMsg.imageFilePathsList.mapNotNull { path -> BitmapFactory.decodeFile(path) }
        if (bitmaps.isNotEmpty()) {
          ChatMessageImage(
            bitmaps = bitmaps,
            imageBitMaps = bitmaps.map { it.asImageBitmap() },
            side = side,
            latencyMs = protoMsg.latencyMs,
            accelerator = protoMsg.accelerator,
            hideSenderLabel = protoMsg.hideSenderLabel,
            persistedPaths = protoMsg.imageFilePathsList.toList(),
          )
        } else {
          null
        }
      }
      "AUDIO_CLIP" -> {
        val firstAudio = protoMsg.audioClipsList.firstOrNull()
        if (firstAudio != null) {
          try {
            ChatMessageAudioClip(
              audioData = File(firstAudio.filePath).readBytes(),
              sampleRate = firstAudio.sampleRate,
              side = side,
              latencyMs = protoMsg.latencyMs,
              persistedPath = firstAudio.filePath,
            )
          } catch (e: Exception) {
            null
          }
        } else {
          null
        }
      }
      else -> null
    }
  }
}

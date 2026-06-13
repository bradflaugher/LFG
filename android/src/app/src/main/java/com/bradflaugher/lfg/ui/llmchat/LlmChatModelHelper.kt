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
package com.bradflaugher.lfg.ui.llmchat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bradflaugher.lfg.common.cleanUpMediapipeTaskErrorMessage
import com.bradflaugher.lfg.data.Accelerator
import com.bradflaugher.lfg.data.ConfigKeys
import com.bradflaugher.lfg.data.DEFAULT_MAX_TOKEN
import com.bradflaugher.lfg.data.DEFAULT_TEMPERATURE
import com.bradflaugher.lfg.data.DEFAULT_TOPK
import com.bradflaugher.lfg.data.DEFAULT_TOPP
import com.bradflaugher.lfg.data.DEFAULT_VISION_ACCELERATOR
import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.data.ModelCapability
import com.bradflaugher.lfg.runtime.CleanUpListener
import com.bradflaugher.lfg.runtime.LlmModelHelper
import com.bradflaugher.lfg.runtime.ResultListener
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.bradflaugher.lfg.data.DataStoreRepository
import org.json.JSONObject
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

private const val TAG = "AGLlmChatModelHelper"

data class LlmModelInstance(val engine: Engine, var conversation: Conversation)

object LlmChatModelHelper : LlmModelHelper {
  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface LlmChatModelHelperEntryPoint {
    fun dataStoreRepository(): DataStoreRepository
  }

  private fun entryPoint(context: Context): LlmChatModelHelperEntryPoint {
    return EntryPointAccessors.fromApplication(
      context.applicationContext,
      LlmChatModelHelperEntryPoint::class.java,
    )
  }

  private var appContext: Context? = null

  // Indexed by model name.
  private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

  @OptIn(ExperimentalApi::class) // opt-in experimental flags
  override fun initialize(
    context: Context,
    model: Model,
    taskId: String,
    supportImage: Boolean,
    supportAudio: Boolean,
    onDone: (String) -> Unit,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    coroutineScope: CoroutineScope?,
  ) {
    appContext = context.applicationContext
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      model.instance = "CloudLlmModelInstance"
      onDone("")
      return
    }

    // Prepare options.
    val maxTokens =
      model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = DEFAULT_MAX_TOKEN)
    val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
    val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
    val temperature =
      model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
    val accelerator =
      model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = Accelerator.GPU.label)
    val visionAccelerator =
      model.getStringConfigValue(
        key = ConfigKeys.VISION_ACCELERATOR,
        defaultValue = DEFAULT_VISION_ACCELERATOR.label,
      )
    val visionBackend =
      when (visionAccelerator) {
        Accelerator.CPU.label -> Backend.CPU()
        Accelerator.GPU.label -> Backend.GPU()
        Accelerator.NPU.label ->
          Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        Accelerator.TPU.label ->
          Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        else -> Backend.GPU()
      }
    val shouldEnableImage = supportImage
    val shouldEnableAudio = supportAudio
    val preferredBackend =
      when (accelerator) {
        Accelerator.CPU.label -> Backend.CPU()
        Accelerator.GPU.label -> Backend.GPU()
        Accelerator.NPU.label ->
          Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        Accelerator.TPU.label ->
          Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        else -> Backend.CPU()
      }
    Log.d(TAG, "Preferred backend: $preferredBackend")

    val modelPath = model.getPath(context = context)
    val engineConfig =
      EngineConfig(
        modelPath = modelPath,
        backend = preferredBackend,
        visionBackend = if (shouldEnableImage) visionBackend else null, // must be GPU for Gemma 3n
        audioBackend = if (shouldEnableAudio) Backend.CPU() else null, // must be CPU for Gemma 3n
        maxNumTokens = maxTokens,
        cacheDir =
          if (modelPath.startsWith("/data/local/tmp")) {
            context.getExternalFilesDir(null)?.absolutePath
          } else {
            null
          },
      )

    // Check if the model file supports speculative decoding.
    var supportsSpeculativeDecoding = false
    // Check if the model file supports speculative decoding.
    try {
      com.google.ai.edge.litertlm.Capabilities(modelPath).use {
        supportsSpeculativeDecoding = it.hasSpeculativeDecodingSupport()
      }
    } catch (e: Exception) {
      // Ignore exceptions and assume not supported.
    }
    // Create an instance of LiteRT LM engine and conversation.
    try {
      var speculativeDecoding = false
      // Check if the model supports speculative decoding for the given task type and if the
      // speculative decoding is enabled in the settings.
      if (
        supportsSpeculativeDecoding &&
        model.capabilityToTaskTypes[ModelCapability.SPECULATIVE_DECODING]?.contains(taskId) ==
        true
      ) {
        speculativeDecoding =
          model.getBooleanConfigValue(
            key = ConfigKeys.ENABLE_SPECULATIVE_DECODING,
            defaultValue = false,
          )
      }
      ExperimentalFlags.enableSpeculativeDecoding = speculativeDecoding
      Log.d(TAG, "Speculative decoding enabled: $speculativeDecoding")
      val engine = Engine(engineConfig)
      engine.initialize()
      ExperimentalFlags.enableSpeculativeDecoding = false

      ExperimentalFlags.enableConversationConstrainedDecoding =
        enableConversationConstrainedDecoding
      val conversation =
        engine.createConversation(
          ConversationConfig(
            samplerConfig =
              if (preferredBackend is Backend.NPU) {
                null
              } else {
                SamplerConfig(
                  topK = topK,
                  topP = topP.toDouble(),
                  temperature = temperature.toDouble(),
                )
              },
            systemInstruction = systemInstruction,
            tools = tools,
          ),
        )
      ExperimentalFlags.enableConversationConstrainedDecoding = false
      model.instance = LlmModelInstance(engine = engine, conversation = conversation)
    } catch (e: Exception) {
      onDone(cleanUpMediapipeTaskErrorMessage(e.message ?: "Unknown error"))
      return
    }
    onDone("")
  }

  @OptIn(ExperimentalApi::class) // opt-in experimental flags
  override fun resetConversation(
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    initialMessages: List<Message>,
  ) {
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      return
    }
    try {
      Log.d(TAG, "Resetting conversation for model '${model.name}'")

      val instance = model.instance as LlmModelInstance? ?: return
      instance.conversation.close()

      val engine = instance.engine
      val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
      val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
      val temperature =
        model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
      val shouldEnableImage = supportImage
      val shouldEnableAudio = supportAudio
      Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")

      val accelerator =
        model.getStringConfigValue(
          key = ConfigKeys.ACCELERATOR,
          defaultValue = Accelerator.GPU.label,
        )
      ExperimentalFlags.enableConversationConstrainedDecoding =
        enableConversationConstrainedDecoding
      val newConversation =
        engine.createConversation(
          ConversationConfig(
            samplerConfig =
              if (accelerator == Accelerator.NPU.label || accelerator == Accelerator.TPU.label) {
                null
              } else {
                SamplerConfig(
                  topK = topK,
                  topP = topP.toDouble(),
                  temperature = temperature.toDouble(),
                )
              },
            systemInstruction = systemInstruction,
            tools = tools,
            initialMessages = initialMessages,
          ),
        )
      ExperimentalFlags.enableConversationConstrainedDecoding = false
      instance.conversation = newConversation

      Log.d(TAG, "Resetting done")
    } catch (e: Exception) {
      Log.d(TAG, "Failed to reset conversation", e)
    }
  }

  override fun cleanUp(
    model: Model,
    onDone: () -> Unit,
  ) {
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      model.instance = null
      onDone()
      return
    }
    if (model.instance == null) {
      return
    }

    val instance = model.instance as LlmModelInstance

    try {
      instance.conversation.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the conversation: ${e.message}")
    }

    try {
      instance.engine.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the engine: ${e.message}")
    }

    val onCleanUp = cleanUpListeners.remove(model.name)
    if (onCleanUp != null) {
      onCleanUp()
    }
    model.instance = null

    onDone()
    Log.d(TAG, "Clean up done.")
  }

  override fun stopResponse(model: Model) {
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      return
    }
    val instance = model.instance as? LlmModelInstance ?: return
    instance.conversation.cancelProcess()
  }

  override fun runInference(
    model: Model,
    input: String,
    resultListener: ResultListener,
    cleanUpListener: CleanUpListener,
    onError: (message: String) -> Unit,
    images: List<Bitmap>,
    audioClips: List<ByteArray>,
    coroutineScope: CoroutineScope?,
    extraContext: Map<String, String>?,
  ) {
    if (model.name == "Cloud-Model-OpenAI-Compatible") {
      val ctx = appContext
      if (ctx == null) {
        onError("Application context is not set.")
        return
      }
      val entryPoint = entryPoint(ctx)
      val endpointRaw = entryPoint.dataStoreRepository().readSecret("CLOUD_API_ENDPOINT")?.trim()
      val endpoint = if (endpointRaw.isNullOrEmpty()) "https://hyper.charm.land/v1/" else endpointRaw
      val apiKey = entryPoint.dataStoreRepository().readSecret("CLOUD_API_KEY")?.trim() ?: ""
      val modelIdRaw = entryPoint.dataStoreRepository().readSecret("CLOUD_MODEL_ID")?.trim()
      val modelId = if (modelIdRaw.isNullOrEmpty()) "gemma-4-26b-a4b-it" else modelIdRaw

      if (apiKey.isEmpty()) {
        onError("Cloud API Key is not configured. Go to the Models view and tap 'Configure API' to enter your API key.")
        return
      }

      if (endpoint.isEmpty()) {
        onError("Cloud API Endpoint is not configured. Go to the Models view and tap 'Configure API'.")
        return
      }

      val historyJson = extraContext?.get("history")
      val chatMessages = mutableListOf<ChatMessage>()
      if (!historyJson.isNullOrEmpty()) {
        try {
          val array = org.json.JSONArray(historyJson)
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val roleStr = obj.getString("role")
            val content = obj.getString("content")
            val role = when (roleStr) {
              "user" -> ChatRole.User
              "assistant" -> ChatRole.Assistant
              "system" -> ChatRole.System
              else -> ChatRole.User
            }
            chatMessages.add(ChatMessage(role = role, content = content))
          }
        } catch (e: Exception) {
          chatMessages.add(ChatMessage(role = ChatRole.User, content = input))
        }
      } else {
        chatMessages.add(ChatMessage(role = ChatRole.User, content = input))
      }

      val maxTokens = model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = DEFAULT_MAX_TOKEN)
      val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
      val temperature = model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)

      coroutineScope?.launch(Dispatchers.IO) {
        try {
          val url = java.net.URL(endpoint.removeSuffix("/") + "/chat/completions")
          val connection = url.openConnection() as java.net.HttpURLConnection
          connection.requestMethod = "POST"
          connection.doOutput = true
          connection.setRequestProperty("Content-Type", "application/json")
          connection.setRequestProperty("Authorization", "Bearer $apiKey")
          connection.setRequestProperty("Accept", "text/event-stream")
          connection.connectTimeout = 30000
          connection.readTimeout = 30000

          val messagesArray = org.json.JSONArray()
          for (msg in chatMessages) {
            val roleStr = when (msg.role) {
              ChatRole.User -> "user"
              ChatRole.Assistant -> "assistant"
              ChatRole.System -> "system"
              else -> "user"
            }
            messagesArray.put(org.json.JSONObject().apply {
              put("role", roleStr)
              put("content", msg.content)
            })
          }

          val payload = org.json.JSONObject().apply {
            put("model", if (modelId.isNotEmpty()) modelId else "gemma-4-2b-it")
            put("messages", messagesArray)
            put("temperature", temperature.toDouble())
            put("top_p", topP.toDouble())
            put("max_tokens", maxTokens)
            put("stream", true)
            put("stream_options", org.json.JSONObject().put("include_usage", true))
          }

          val writer = java.io.OutputStreamWriter(connection.outputStream)
          writer.write(payload.toString())
          writer.flush()
          writer.close()

          val responseCode = connection.responseCode
          if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
              val lineStr = line?.trim() ?: ""
              if (lineStr.startsWith("data:")) {
                val data = lineStr.removePrefix("data:").trim()
                if (data == "[DONE]") {
                  break
                }
                if (data.isEmpty()) continue

                try {
                  val jsonObj = org.json.JSONObject(data)
                  
                  // Extract prompt caching usage info if present in chunk
                  val usageObj = jsonObj.optJSONObject("usage")
                  if (usageObj != null) {
                    val promptDetails = usageObj.optJSONObject("prompt_tokens_details")
                    if (promptDetails != null) {
                      val cached = promptDetails.optInt("cached_tokens", 0)
                      val prompt = usageObj.optInt("prompt_tokens", 0)
                      if (prompt > 0) {
                        model.lastCacheHitPercentage = cached.toFloat() / prompt.toFloat()
                      }
                    }
                  }

                  // Extract and stream content chunk
                  val choices = jsonObj.optJSONArray("choices")
                  if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val delta = choice.optJSONObject("delta")
                    val content = delta?.optString("content", "") ?: ""
                    if (content.isNotEmpty()) {
                      withContext(Dispatchers.Main) {
                        resultListener(content, false, null)
                      }
                    }
                  }
                } catch (e: Exception) {
                  // Ignore JSON parse errors for non-object/marker chunks
                }
              }
            }
            reader.close()
            withContext(Dispatchers.Main) {
              resultListener("", true, null)
              cleanUpListener()
            }
          } else {
            val errorReader = java.io.BufferedReader(java.io.InputStreamReader(connection.errorStream ?: connection.inputStream))
            val errorResponse = java.lang.StringBuilder()
            var line: String?
            while (errorReader.readLine().also { line = it } != null) {
              errorResponse.append(line)
            }
            errorReader.close()
            withContext(Dispatchers.Main) {
              onError("Cloud API Error: $responseCode - $errorResponse")
              cleanUpListener()
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Cloud API call failed", e)
          withContext(Dispatchers.Main) {
            onError("Cloud API Call failed: ${e.message}")
            cleanUpListener()
          }
        }
      }
      return
    }
    val instance = model.instance as? LlmModelInstance
    if (instance == null) {
      onError("LlmModelInstance is not initialized.")
      return
    }

    // Set listener.
    if (!cleanUpListeners.containsKey(model.name)) {
      cleanUpListeners[model.name] = cleanUpListener
    }

    val conversation = instance.conversation

    val contents = mutableListOf<Content>()
    for (image in images) {
      contents.add(Content.ImageBytes(image.toPngByteArray()))
    }
    for (audioClip in audioClips) {
      contents.add(Content.AudioBytes(audioClip))
    }
    // add the text after image and audio for the accurate last token
    if (input.trim().isNotEmpty()) {
      contents.add(Content.Text(input))
    }

    conversation.sendMessageAsync(
      Contents.of(contents),
      object : MessageCallback {
        override fun onMessage(message: Message) {
          resultListener(message.toString(), false, message.channels["thought"])
        }

        override fun onDone() {
          resultListener("", true, null)
        }

        override fun onError(throwable: Throwable) {
          if (throwable is CancellationException) {
            Log.i(TAG, "The inference is cancelled.")
            resultListener("", true, null)
          } else {
            Log.e(TAG, "onError", throwable)
            onError("Error: ${throwable.message}")
          }
        }
      },
      extraContext ?: emptyMap(),
    )
  }

  private fun Bitmap.toPngByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
  }
}

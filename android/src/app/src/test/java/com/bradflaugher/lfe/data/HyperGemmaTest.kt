/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.data

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class HyperGemmaTest {

  @Test
  fun testHyperGemmaChatCompletion() = runBlocking {
    val apiKey = System.getenv("HYPER_API_KEY")
    assumeTrue(
      "Skipping Hyper Gemma Cloud test: HYPER_API_KEY not found in environment",
      apiKey != null && apiKey.isNotEmpty()
    )

    val envEndpoint = System.getenv("HYPER_API_ENDPOINT")
    val endpoint = if (envEndpoint.isNullOrEmpty()) {
      "https://api.hyper.space/v1/"
    } else {
      var ep = envEndpoint.trim()
      if (ep.endsWith("/chat/completions")) {
        ep = ep.removeSuffix("/chat/completions")
      } else if (ep.endsWith("/chat/completions/")) {
        ep = ep.removeSuffix("/chat/completions/")
      }
      if (!ep.endsWith("/")) {
        ep = "$ep/"
      }
      ep
    }
    val modelId = "gemma-4-2b-it"

    try {
      val config = OpenAIConfig(
        token = apiKey!!,
        host = OpenAIHost(endpoint),
        timeout = Timeout(socket = 30.seconds, connect = 15.seconds)
      )
      val openAI = OpenAI(config)

      val request = ChatCompletionRequest(
        model = ModelId(modelId),
        messages = listOf(
          ChatMessage(
            role = ChatRole.User,
            content = "Hello! Reply with exactly the word SUCCESS."
          )
        )
      )

      val response = openAI.chatCompletion(request)
      val responseText = response.choices.firstOrNull()?.message?.content
      assertNotNull("Response message content should not be null", responseText)
      println("Hyper Gemma SDK Response: $responseText")
      assertTrue("Response should contain choices and not be empty", !responseText.isNullOrEmpty())
    } catch (e: Exception) {
      val msg = e.message ?: "Unknown error"
      println("Hyper Gemma Cloud test failed with exception: $msg")
      assumeTrue(
        "Skipping Hyper Gemma Cloud test: Remote call failed (server might be down or key invalid): $msg",
        false
      )
    }
  }
}

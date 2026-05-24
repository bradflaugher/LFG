/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class HyperGemmaTest {

  @Test
  fun testHyperGemmaChatCompletion() {
    val apiKey = System.getenv("HYPER_API_KEY")
    assumeTrue(
      "Skipping Hyper Gemma Cloud test: HYPER_API_KEY not found in environment",
      apiKey != null && apiKey.isNotEmpty()
    )

    val endpoint = "https://api.hyper.space/v1/chat/completions"
    val modelId = "gemma-4-2b-it"

    val requestBody = """
      {
        "model": "$modelId",
        "messages": [
          {"role": "user", "content": "Hello! Reply with exactly the word SUCCESS."}
        ],
        "temperature": 0.1,
        "max_tokens": 10
      }
    """.trimIndent()

    var connection: HttpURLConnection? = null
    try {
      val url = URL(endpoint)
      connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Authorization", "Bearer $apiKey")
      connection.doOutput = true
      connection.doInput = true
      connection.connectTimeout = 15000
      connection.readTimeout = 15000

      connection.outputStream.use { os ->
        os.write(requestBody.toByteArray(Charsets.UTF_8))
      }

      val responseCode = connection.responseCode
      assertTrue("Response code should be 200: got $responseCode", responseCode == 200)

      val response = connection.inputStream.bufferedReader().use { it.readText() }
      assertNotNull("Response should not be null", response)
      assertTrue("Response should contain choices block", response.contains("choices"))
    } finally {
      connection?.disconnect()
    }
  }
}

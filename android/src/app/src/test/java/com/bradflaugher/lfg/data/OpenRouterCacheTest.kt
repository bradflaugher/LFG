package com.bradflaugher.lfg.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterCacheTest {

  @Test
  fun testOpenRouterPromptCaching() = runBlocking {
    val apiKey = System.getenv("OPENROUTER_API_KEY")
    assumeTrue(
      "Skipping OpenRouter Caching test: OPENROUTER_API_KEY not found in environment",
      apiKey != null && apiKey.isNotEmpty()
    )

    val endpoint = "https://openrouter.ai/api/v1/chat/completions"
    val modelId = "~openai/gpt-mini-latest"

    // Construct a large prompt prefix (~2500 words / ~3500 tokens)
    val prefixText = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et " +
        "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
        "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore " +
        "eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia " +
        "deserunt mollit anim id est laborum. ").repeat(80)

    println("=" .repeat(60))
    print("Testing OpenAI Implicit Caching on OpenRouter using raw HTTP connections\n")
    println("=" .repeat(60))

    // Run Request 1: Write to cache
    println("\n--- Request 1: Write to Cache ---")
    val response1 = sendRequest(endpoint, apiKey!!, modelId, prefixText, "First prompt question: Summarize the main themes of the text above in one sentence.")
    assertNotNull("Request 1 response should not be null", response1)
    println("Request 1 Full Response: $response1")
    
    val writeTokens1 = parseTokenCount(response1 ?: "", "cache_write_tokens")
    println("Request 1 Cache Write Tokens: $writeTokens1")

    // Run Request 2: Read from cache
    println("\n--- Request 2: Read from Cache ---")
    val response2 = sendRequest(endpoint, apiKey, modelId, prefixText, "Second prompt question: What is the first word of the text above?")
    assertNotNull("Request 2 response should not be null", response2)
    println("Request 2 Full Response: $response2")
    
    val cachedTokens2 = parseTokenCount(response2 ?: "", "cached_tokens")
    println("Request 2 Cache Hit Tokens: $cachedTokens2")
    
    assertTrue("Cache hit tokens should be greater than zero on consecutive requests with exact prefix", cachedTokens2 > 0)
    println("SUCCESS: Verified cache hits on OpenRouter! Received $cachedTokens2 cached tokens.")
  }

  private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t")
  }

  private fun parseTokenCount(jsonResponse: String, keyName: String): Int {
    val regex = "\"$keyName\"\\s*:\\s*(\\d+)".toRegex()
    val match = regex.find(jsonResponse)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
  }

  private fun sendRequest(endpointUrl: String, apiKey: String, model: String, prefix: String, question: String): String? {
    try {
      val url = URL(endpointUrl)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Authorization", "Bearer $apiKey")
      connection.setRequestProperty("HTTP-Referer", "https://github.com/bradflaugher/LFG")
      connection.setRequestProperty("X-Title", "LFG Test Suite")
      connection.connectTimeout = 45000
      connection.readTimeout = 45000

      // Create raw JSON payload using standard OpenAI implicit prompt caching format
      val escapedPrefix = escapeJson(prefix)
      val escapedQuestion = escapeJson(question)
      val payload = """
      {
        "model": "$model",
        "temperature": 0.0,
        "messages": [
          {
            "role": "user",
            "content": "$escapedPrefix\n\n$escapedQuestion"
          }
        ]
      }
      """.trimIndent()

      val writer = OutputStreamWriter(connection.outputStream)
      writer.write(payload)
      writer.flush()
      writer.close()

      val responseCode = connection.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          response.append(line)
        }
        reader.close()
        return response.toString()
      } else {
        val errorReader = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream))
        val errorResponse = StringBuilder()
        var line: String?
        while (errorReader.readLine().also { line = it } != null) {
          errorResponse.append(line)
        }
        errorReader.close()
        println("HTTP error $responseCode: $errorResponse")
      }
    } catch (e: Exception) {
      println("Request failed with exception:")
      e.printStackTrace()
    }
    return null
  }
}

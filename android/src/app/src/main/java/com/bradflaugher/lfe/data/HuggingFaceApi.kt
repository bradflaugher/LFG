/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "AGHuggingFaceApi"

/** A subset of the HF /api/models response — only the fields we use. */
data class HfModelInfo(
  @SerializedName("id") val id: String, // e.g. "litert-community/Gemma3-1B-IT"
  @SerializedName("author") val author: String? = null,
  @SerializedName("downloads") val downloads: Long = 0,
  @SerializedName("likes") val likes: Long = 0,
  @SerializedName("lastModified") val lastModified: String? = null,
  @SerializedName("pipeline_tag") val pipelineTag: String? = null,
  @SerializedName("tags") val tags: List<String> = emptyList(),
  /** When `full=true` is set, HF includes the list of files in the repo. */
  @SerializedName("siblings") val siblings: List<HfSibling> = emptyList(),
  @SerializedName("gated") val gated: Any? = null,
) {
  val displayName: String get() = id.substringAfter('/')

  /**
   * Heuristic: this repo is runnable in LFE if it ships a file in a LiteRT
   * format (`.litertlm`, `.task`). Anything else (raw `.safetensors`,
   * `.gguf`, etc.) won't load via the LiteRT-LM runtime.
   */
  val isLfeCompatible: Boolean
    get() = siblings.any { it.isLfeRuntimeFile }

  /** Files that LFE can actually execute. */
  val runtimeFiles: List<HfSibling>
    get() = siblings.filter { it.isLfeRuntimeFile }
}

data class HfSibling(
  @SerializedName("rfilename") val rfilename: String,
  @SerializedName("size") val size: Long? = null,
) {
  val isLfeRuntimeFile: Boolean
    get() {
      val lower = rfilename.lowercase()
      return lower.endsWith(".litertlm") || lower.endsWith(".task")
    }
}

/** Response wrapper used by [HuggingFaceApi.searchModels]. */
data class HfSearchPage(
  val models: List<HfModelInfo>,
  val nextLink: String?,
)

/**
 * Tiny client for the HuggingFace `/api/models` endpoint.
 *
 * We don't pull in Ktor or Retrofit for this — the gallery codebase already
 * uses `HttpURLConnection` + Gson, so we stick to one HTTP story.
 *
 * If you want to add gated-model support, pass `authToken` (read from
 * `DataStoreRepository.readAccessTokenData()`).
 */
object HuggingFaceApi {
  private const val BASE = "https://huggingface.co/api/models"

  /**
   * Hit `/api/models` with the given filters. When [nextPageUrl] is non-null
   * we follow the paginated Link header instead of building a fresh query —
   * HF returns the next page URL fully constructed.
   */
  fun searchModels(
    search: String? = null,
    author: String? = null,
    filter: String? = null,
    sort: String = "downloads",
    direction: String = "-1",
    limit: Int = 25,
    full: Boolean = true,
    nextPageUrl: String? = null,
    authToken: String? = null,
  ): HfSearchPage? {
    val url = nextPageUrl ?: buildSearchUrl(search, author, filter, sort, direction, limit, full)
    Log.d(TAG, "GET $url")

    var connection: HttpURLConnection? = null
    try {
      connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 15000
        if (!authToken.isNullOrEmpty()) {
          setRequestProperty("Authorization", "Bearer $authToken")
        }
      }
      val code = connection.responseCode
      if (code != HttpURLConnection.HTTP_OK) {
        Log.w(TAG, "HF API returned HTTP $code")
        return null
      }
      val body = connection.inputStream.bufferedReader().use { it.readText() }
      val models: List<HfModelInfo> =
        try {
          Gson().fromJson(body, Array<HfModelInfo>::class.java).toList()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse HF models JSON", e)
          return null
        }

      val nextLink = parseNextLinkHeader(connection.getHeaderField("Link"))
      return HfSearchPage(models = models, nextLink = nextLink)
    } catch (e: IOException) {
      Log.e(TAG, "HF API request failed", e)
      return null
    } finally {
      connection?.disconnect()
    }
  }

  internal fun buildSearchUrl(
    search: String?,
    author: String?,
    filter: String?,
    sort: String,
    direction: String,
    limit: Int,
    full: Boolean,
  ): String {
    val params = mutableListOf<Pair<String, String>>()
    if (!search.isNullOrBlank()) params += "search" to search
    if (!author.isNullOrBlank()) params += "author" to author
    if (!filter.isNullOrBlank()) params += "filter" to filter
    params += "sort" to sort
    params += "direction" to direction
    params += "limit" to limit.toString()
    if (full) params += "full" to "true"
    val query = params.joinToString("&") { (k, v) ->
      "$k=${URLEncoder.encode(v, "UTF-8")}"
    }
    return "$BASE?$query"
  }

  /** Parse `<https://...>; rel="next"` style Link headers. */
  internal fun parseNextLinkHeader(header: String?): String? {
    if (header.isNullOrBlank()) return null
    return Regex("<([^>]+)>;\\s*rel=\"next\"").find(header)?.groupValues?.getOrNull(1)
  }
}

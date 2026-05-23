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
package com.bradflaugher.lfe.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CompletableDeferred

interface LatencyProvider {
  val latencyMs: Float
}

data class Classification(val label: String, val score: Float, val color: Color)

data class JsonObjAndTextContent<T>(val jsonObj: T, val textContent: String)

class AudioClip(val audioData: ByteArray, val sampleRate: Int)

open class AgentAction(val name: AgentActionName)

class CallJsAgentAction(
  val url: String,
  val data: String,
  val secret: String = "",
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.CALL_JS_SKILL)

class AskInfoAgentAction(
  val dialogTitle: String,
  val fieldLabel: String,
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.ASK_INFO)

class SkillProgressAgentAction(
  val label: String,
  val inProgress: Boolean,
  val addItemTitle: String = "",
  val addItemDescription: String = "",
  val customData: Any? = null,
) : AgentAction(name = AgentActionName.SKILL_PROGRESS)

// Request Android permission to perform certain actions, e.g. read calendar events.
class RequestPermissionAgentAction(
  val permission: String,
  val result: CompletableDeferred<Boolean> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.REQUEST_PERMISSION)

enum class AgentActionName() {
  CALL_JS_SKILL,
  SKILL_PROGRESS,
  ASK_INFO,
  REQUEST_PERMISSION,
}

data class SkillTryOutChip(
  val icon: ImageVector,
  val label: String,
  val prompt: String,
  val skillName: String,
)

data class SkillInfo(
  val skillMd: String,
  val skillUrl: String? = null,
  val tryoutChip: SkillTryOutChip? = null,
)

data class SkillsIndex(val skills: List<SkillInfo>)

@JsonClass(generateAdapter = true)
data class CallJsSkillResult(
  val result: String?,
  val error: String?,
  val image: CallJsSkillResultImage?,
  val webview: CallJsSkillResultWebview?,
)

@JsonClass(generateAdapter = true)
data class CallJsSkillResultImage(val base64: String?)

@JsonClass(generateAdapter = true)
data class CallJsSkillResultWebview(
  val url: String?,
  val iframe: Boolean?,
  // width/height.
  //
  // In the app the webview always takes the full width of the screen. This value is used to
  // calculate the height of the webview. Default is 4:3.
  val aspectRatio: Float?,
)

package com.bradflaugher.lfg.common

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

// Fetch an article URL via a hidden WebView (so any cookies persisted from a
// prior in-app sign-in are attached), then run Readability.js to extract title
// + main text. Result is JSON: {"title": "...", "text": "..."} or {"error": "..."}.
class FetchArticleAgentAction(
  val url: String,
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.FETCH_ARTICLE)

// Extract a deduped list of anchor links from a page (used for surfacing
// article candidates off a homepage). Result is JSON:
// {"url": "...", "links": [{"text": "...", "href": "..."}]} or {"error": "..."}.
class FetchLinksAgentAction(
  val url: String,
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.FETCH_LINKS)

// Load a URL, click an element, wait, and extract title + main text using Readability.
class ClickAndReadWebpageAgentAction(
  val url: String,
  val selector: String,
  val result: CompletableDeferred<String> = CompletableDeferred(),
) : AgentAction(name = AgentActionName.CLICK_AND_READ_WEBPAGE)

enum class AgentActionName() {
  CALL_JS_SKILL,
  SKILL_PROGRESS,
  ASK_INFO,
  REQUEST_PERMISSION,
  FETCH_ARTICLE,
  FETCH_LINKS,
  CLICK_AND_READ_WEBPAGE,
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

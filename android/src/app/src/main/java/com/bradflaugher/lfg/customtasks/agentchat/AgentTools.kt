package com.bradflaugher.lfg.customtasks.agentchat

import android.content.Context
import android.util.Log
import com.bradflaugher.lfg.common.AgentAction
import com.bradflaugher.lfg.common.AskInfoAgentAction
import com.bradflaugher.lfg.common.CallJsAgentAction
import com.bradflaugher.lfg.common.CallJsSkillResult
import com.bradflaugher.lfg.common.CallJsSkillResultImage
import com.bradflaugher.lfg.common.CallJsSkillResultWebview
import com.bradflaugher.lfg.common.ClickAndReadWebpageAgentAction
import com.bradflaugher.lfg.common.FetchArticleAgentAction
import com.bradflaugher.lfg.common.FetchLinksAgentAction
import com.bradflaugher.lfg.common.LOCAL_URL_BASE
import com.bradflaugher.lfg.common.RequestPermissionAgentAction
import com.bradflaugher.lfg.common.SkillProgressAgentAction
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking

private const val TAG = "AGAgentTools"

open class AgentTools() : ToolSet {
  lateinit var context: Context
  lateinit var skillManagerViewModel: SkillManagerViewModel
  lateinit var taskId: String

  private val _actionChannel = Channel<AgentAction>(Channel.UNLIMITED)
  val actionChannel: ReceiveChannel<AgentAction> = _actionChannel
  var resultImageToShow: CallJsSkillResultImage? = null
  var resultWebviewToShow: CallJsSkillResultWebview? = null

  /** Loads skill. */
  @Tool(description = "Loads a skill.")
  fun loadSkill(
    @ToolParam(description = "The name of the skill to load.") skillName: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }
      val skillContent =
        if (skill != null) {
          "---\nname: ${skill.name}\ndescription: ${skill.description}\n---\n\n${skill.instructions}"
        } else {
          "Skill not found"
        }
      Log.d(TAG, "load skill. Skill content:\n$skillContent")
      if (skill != null) {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Loading skill \"$skillName\"",
            inProgress = true,
            addItemTitle = "Load \"${skill.name}\"",
            addItemDescription = "Description: ${skill.description}",
            customData = skill,
          ),
        )
      } else {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Failed to load skill \"$skillName\"",
            inProgress = false,
          ),
        )
      }

      mapOf("skill_name" to skillName, "skill_instructions" to skillContent)
    }
  }

  /** Call JS skill */
  @Tool(description = "Runs JS script")
  fun runJs(
    @ToolParam(description = "The name of skill") skillName: String,
    @ToolParam(description = "The script name to run. Use 'index.html' if not provided by user")
    scriptName: String,
    @ToolParam(
      description = "The data to pass to the script. Use empty string if not provided by user",
    )
    data: String,
  ): Map<String, Any> {
    return runBlocking(Dispatchers.Default) {
      Log.d(
        TAG,
        "runJS tool called with:" +
          "\n- skillName: ${skillName}\n- scriptName: ${scriptName}\n- data: ${data}\n",
      )

      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }

      if (skill == null) {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Failed to call skill \"$scriptName\"",
            inProgress = false,
          ),
        )
        return@runBlocking mapOf(
          "error" to "Skill \"${scriptName}\" not found",
          "status" to "failed",
        )
      }

      // Check secret. If a skill requires a secret and the secret is not provided, show error.
      var secret = ""
      if (skill.requireSecret) {
        val savedSecret =
          skillManagerViewModel.dataStoreRepository.readSecret(
            key = getSkillSecretKey(skillName = skillName),
          )
        if (savedSecret == null || savedSecret.isEmpty()) {
          val action =
            AskInfoAgentAction(
              dialogTitle = "Enter secret",
              fieldLabel =
                skill.requireSecretDescription.ifEmpty {
                  "The JS script needs a secret (API key / token) to proceed:"
                },
            )
          _actionChannel.send(action)
          secret = action.result.await()
          if (secret.isNotEmpty()) {
            skillManagerViewModel.dataStoreRepository.saveSecret(
              key = getSkillSecretKey(skillName = skillName),
              value = secret,
            )
            Log.d(TAG, "Got Secret from ask info dialog: ${secret.substring(0, 3)}")
          } else {
            Log.d(TAG, "The ask info dialog got cancelled. No secret.")
          }
        } else {
          secret = savedSecret
        }
      }

      // Get the url for the skill.
      val url =
        skillManagerViewModel.getJsSkillUrl(skillName = skillName, scriptName = scriptName)
          ?: return@runBlocking mapOf(
            "result" to "JS Skill URL not set properly or skill not found",
          )
      Log.d(TAG, "Calling JS script.\n- url: $url\n- data: $data")

      // Update progress.
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Calling JS script \"$skillName/${scriptName}\"",
          inProgress = true,
          addItemTitle = "Call JS script: \"$skillName/${scriptName}\"",
          addItemDescription = "- URL: ${url.replace(LOCAL_URL_BASE, "")}\n- Data: $data",
          customData = skill,
        ),
      )

      // Actually run it and wait for the result.
      val action =
        CallJsAgentAction(url = url, data = data.trim().ifEmpty { "{}" }, secret = secret)
      _actionChannel.send(action)
      val result = action.result.await()

      // Try to parse result to CallJsSkillResult.
      val moshi: Moshi = Moshi.Builder().build()
      val jsonAdapter: JsonAdapter<CallJsSkillResult> =
        moshi.adapter(CallJsSkillResult::class.java).failOnUnknown()
      val resultJson = runCatching { jsonAdapter.fromJson(result) }.getOrNull()
      val error = resultJson?.error

      // Failed to parse. Treat its whole as a result string.
      if (
        resultJson == null ||
        (resultJson.result == null && resultJson.webview == null && resultJson.image == null)
      ) {
        mapOf("result" to result, "status" to "succeeded")
      }
      // Error case.
      else if (error != null) {
        mapOf("error" to error, "status" to "failed")
      }
      // Non-error cases.
      else {
        // Handle image and webview in result.
        val image = resultJson.image
        val webview = resultJson.webview
        if (image != null) {
          Log.d(TAG, "Got an image response.")
          resultImageToShow = image
        }
        if (webview != null) {
          Log.d(TAG, "Got an webview response.")
          val webviewUrl =
            skillManagerViewModel.getJsSkillWebviewUrl(
              skillName = skillName,
              url = webview.url ?: "",
            )
          Log.d(TAG, "Webview url: $webviewUrl")
          resultWebviewToShow = webview.copy(url = webviewUrl)
        }
        Log.d(TAG, "Result: ${resultJson.result}")
        mapOf("result" to (resultJson.result ?: ""), "status" to "succeeded")
      }
    }
  }

  @Tool(
    description =
      "Run an Android intent. It is used to interact with the app to perform certain actions.",
  )
  fun runIntent(
    @ToolParam(description = "The intent to run.") intent: String,
    @ToolParam(
      description = "A JSON string containing the parameter values required for the intent.",
    )
    parameters: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      if (IntentAction.from(intent) == null) {
        Log.w(TAG, "Intent not found: '$intent'")
        return@runBlocking guardMissingEntityWithSkillFallback(name = intent, type = "Intent")
      }
      Log.d(TAG, "Run intent. Intent: '$intent', parameters: '$parameters'")
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Executing intent \"$intent\"",
          inProgress = true,
          addItemTitle = "Execute intent \"$intent\"",
          addItemDescription = "Parameters: $parameters",
        ),
      )
      val res =
        IntentHandler.handleAction(context, intent, parameters) { permission ->
          val permissionAction = RequestPermissionAgentAction(permission = permission)
          _actionChannel.send(permissionAction)
          permissionAction.result.await()
        }
      return@runBlocking mapOf("action" to intent, "parameters" to parameters, "result" to res)
    }
  }

  @Tool(
    description =
      "Fetch a deduped list of in-page links (text + href) from a URL, typically a news " +
        "homepage. Use this to discover article candidates before calling fetchArticle on a " +
        "specific one. Persistent cookies are attached, so it works on signed-in publishers.",
  )
  fun fetchLinks(
    @ToolParam(description = "The full https:// URL of the page to enumerate links on.")
    url: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val trimmed = url.trim()
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return@runBlocking mapOf(
          "error" to "URL must start with http:// or https://",
          "status" to "failed",
        )
      }
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Loading links",
          inProgress = true,
          addItemTitle = "Load links",
          addItemDescription = trimmed,
        ),
      )
      val action = FetchLinksAgentAction(url = trimmed)
      _actionChannel.send(action)
      val resultJson =
        try {
          action.result.await()
        } catch (e: Exception) {
          Log.e(TAG, "fetchLinks failed", e)
          return@runBlocking mapOf("error" to (e.message ?: "fetch failed"), "status" to "failed")
        }
      mapOf("links" to resultJson)
    }
  }

  @Tool(
    description =
      "Fetch the title and main text of an article from a URL via a hidden WebView. " +
        "Works on any publicly readable page. Pages behind a sign-in or paywall will " +
        "fail — surface that to the user in plain language.",
  )
  fun fetchArticle(
    @ToolParam(description = "The full https:// URL of the article to fetch.") url: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val trimmed = url.trim()
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return@runBlocking mapOf(
          "error" to "URL must start with http:// or https://",
          "status" to "failed",
        )
      }
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Fetching article",
          inProgress = true,
          addItemTitle = "Fetch article",
          addItemDescription = trimmed,
        ),
      )
      val action = FetchArticleAgentAction(url = trimmed)
      _actionChannel.send(action)
      val resultJson =
        try {
          action.result.await()
        } catch (e: Exception) {
          Log.e(TAG, "fetchArticle failed", e)
          return@runBlocking mapOf("error" to (e.message ?: "fetch failed"), "status" to "failed")
        }
      // The result-side composable returns either {"title":..., "text":...} or {"error":...}.
      mapOf("article" to resultJson)
    }
  }

  @Tool(
    description =
      "Display an interactive WebView of a webpage/URL to the user within the chat, " +
        "allowing them to view, scroll, click, and interact with the page.",
  )
  fun showWebpage(
    @ToolParam(description = "The full https:// URL of the webpage to display.") url: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val trimmed = url.trim()
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return@runBlocking mapOf(
          "error" to "URL must start with http:// or https://",
          "status" to "failed",
        )
      }
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Opening webpage",
          inProgress = true,
          addItemTitle = "Open webpage",
          addItemDescription = trimmed,
        ),
      )
      resultWebviewToShow =
        CallJsSkillResultWebview(
          url = trimmed,
          iframe = false,
          aspectRatio = 1.0f,
        )
      mapOf("result" to "WebView displayed to the user.", "status" to "succeeded")
    }
  }

  @Tool(
    description =
      "Load a webpage URL, click a link or button (via CSS selector), wait for the page to update, " +
        "and return the updated page's main article text. Use this to click buttons/links, expand menus, " +
        "load more content, or interact dynamically with the page.",
  )
  fun clickAndReadWebpage(
    @ToolParam(description = "The full https:// URL of the webpage.") url: String,
    @ToolParam(description = "The CSS selector of the element to click (e.g. 'button.load-more', 'a.read-more').") selector: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val trimmed = url.trim()
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return@runBlocking mapOf(
          "error" to "URL must start with http:// or https://",
          "status" to "failed",
        )
      }
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Clicking element and reading page",
          inProgress = true,
          addItemTitle = "Click & read webpage",
          addItemDescription = "URL: $trimmed\nSelector: $selector",
        ),
      )
      val action = ClickAndReadWebpageAgentAction(url = trimmed, selector = selector)
      _actionChannel.send(action)
      val resultJson =
        try {
          action.result.await()
        } catch (e: Exception) {
          Log.e(TAG, "clickAndReadWebpage failed", e)
          return@runBlocking mapOf("error" to (e.message ?: "click and read failed"), "status" to "failed")
        }
      mapOf("article" to resultJson)
    }
  }



  /**
   * Guards against missing entities (tools or intents) by checking if they exist as skills. Returns
   * a failure response with a specific hint to the model to try running it as a skill if it is
   * found in the allowed skills list. This helps guide the model when it gets confused and tries to
   * call a skill as a tool or intent.
   */
  private fun guardMissingEntityWithSkillFallback(
    name: String,
    type: String,
  ): Map<String, String> {
    val skills = skillManagerViewModel.getSelectedSkills()
    val isSkill = skills.any { it.name == name.trim() }
    val error = if (isSkill) "$type not found. Try to run it as a skill" else "Tool not found"
    return mapOf("error" to error, "status" to "failed")
  }

  fun sendAgentAction(action: AgentAction) {
    runBlocking(Dispatchers.Default) { _actionChannel.send(action) }
  }
}

fun getSkillSecretKey(skillName: String): String {
  return "skill___$skillName"
}

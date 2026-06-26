package com.bradflaugher.lfg.runtime

import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.ui.llmchat.LlmChatModelHelper

val Model.runtimeHelper: LlmModelHelper
  get() = LlmChatModelHelper

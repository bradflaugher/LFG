/*
 * LFE — Low-Feature Edge agent. Apache 2.0.
 * Forked from Google AI Edge Gallery (Apache 2.0, Copyright 2025-2026 Google LLC).
 */

package com.bradflaugher.lfe.runtime

import com.bradflaugher.lfe.data.Model
import com.bradflaugher.lfe.ui.llmchat.LlmChatModelHelper

val Model.runtimeHelper: LlmModelHelper
  get() = LlmChatModelHelper

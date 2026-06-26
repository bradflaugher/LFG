/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the MIT License.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.runtime

import com.bradflaugher.lfg.data.Model
import com.bradflaugher.lfg.ui.llmchat.LlmChatModelHelper

val Model.runtimeHelper: LlmModelHelper
  get() = LlmChatModelHelper

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
package com.bradflaugher.lfg.ui.common.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageBodyAudioClip(
  message: ChatMessageAudioClip,
  modifier: Modifier = Modifier,
) {
  AudioPlaybackPanel(
    audioData = message.audioData,
    sampleRate = message.sampleRate,
    isRecording = false,
    modifier = Modifier.padding(end = 16.dp),
    onDarkBg = true,
  )
}

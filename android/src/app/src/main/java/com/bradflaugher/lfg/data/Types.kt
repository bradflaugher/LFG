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
package com.bradflaugher.lfg.data

enum class Accelerator(val label: String) {
  CPU(label = "CPU"),
  GPU(label = "GPU"),
  NPU(label = "NPU"),
  TPU(label = "TPU"),
}

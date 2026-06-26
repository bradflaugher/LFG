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

/** Possible action for app bar. */
enum class AppBarActionType {
  NO_ACTION,
  APP_SETTING,
  DOWNLOAD_MANAGER,
  NAVIGATE_UP,
  MENU,
}

class AppBarAction(val actionType: AppBarActionType, val actionFn: () -> Unit)

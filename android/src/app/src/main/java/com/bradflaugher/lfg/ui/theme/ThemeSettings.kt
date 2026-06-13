/*
 * LFG — Uncensored on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 *
 * Includes code adapted from Google AI Edge Gallery (Apache 2.0,
 * Copyright 2025 Google LLC) — https://github.com/google-ai-edge/gallery.
 */
package com.bradflaugher.lfg.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.bradflaugher.lfg.proto.Theme

object ThemeSettings {
  val themeOverride = mutableStateOf<Theme>(Theme.THEME_AUTO)
}

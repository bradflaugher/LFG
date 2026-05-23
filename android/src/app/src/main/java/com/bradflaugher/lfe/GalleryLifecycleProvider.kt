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
package com.bradflaugher.lfe

interface AppLifecycleProvider {
  var isAppInForeground: Boolean
}

class GalleryLifecycleProvider : AppLifecycleProvider {
  private var _isAppInForeground = false

  override var isAppInForeground: Boolean
    get() = _isAppInForeground
    set(value) {
      _isAppInForeground = value
    }
}

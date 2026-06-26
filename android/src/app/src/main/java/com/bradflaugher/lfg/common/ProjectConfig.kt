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
package com.bradflaugher.lfg.common

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration

object ProjectConfig {
  // Hugging Face Client ID.
  //
  const val clientId = "REPLACE_WITH_YOUR_CLIENT_ID_IN_HUGGINGFACE_APP"

  // Registered redirect URI.
  //
  // The scheme needs to match the
  // "android.defaultConfig.manifestPlaceholders["appAuthRedirectScheme"]" field in
  // "build.gradle.kts".
  const val redirectUri = "REPLACE_WITH_YOUR_REDIRECT_URI_IN_HUGGINGFACE_APP"

  // OAuth 2.0 Endpoints (Authorization + Token Exchange)
  private const val authEndpoint = "https://huggingface.co/oauth/authorize"
  private const val tokenEndpoint = "https://huggingface.co/oauth/token"

  // OAuth service configuration (AppAuth library requires this)
  val authServiceConfig =
    AuthorizationServiceConfiguration(
      authEndpoint.toUri(), // Authorization endpoint
      tokenEndpoint.toUri(), // Token exchange endpoint
    )
}

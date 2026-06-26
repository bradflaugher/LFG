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
package com.bradflaugher.lfg

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel
import com.bradflaugher.lfg.ui.navigation.LfgNavHost

/** Top-level composable: bootstraps the LFG nav host. */
@Composable
fun LfgApp(
  navController: NavHostController = rememberNavController(),
  modelManagerViewModel: ModelManagerViewModel,
) {
  LfgNavHost(navController = navController, modelManagerViewModel = modelManagerViewModel)
}

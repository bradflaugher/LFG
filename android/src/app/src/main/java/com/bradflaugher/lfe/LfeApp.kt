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

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bradflaugher.lfe.ui.modelmanager.ModelManagerViewModel
import com.bradflaugher.lfe.ui.navigation.LfeNavHost

/** Top-level composable: bootstraps the LFE nav host. */
@Composable
fun LfeApp(
  navController: NavHostController = rememberNavController(),
  modelManagerViewModel: ModelManagerViewModel,
) {
  LfeNavHost(navController = navController, modelManagerViewModel = modelManagerViewModel)
}

/*
 * LFE — Low-Feature Edge agent. Apache 2.0.
 * Forked from Google AI Edge Gallery (Apache 2.0, Copyright 2025 Google LLC).
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

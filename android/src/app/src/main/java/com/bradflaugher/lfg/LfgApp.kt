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

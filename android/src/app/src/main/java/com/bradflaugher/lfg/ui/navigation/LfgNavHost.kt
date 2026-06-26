package com.bradflaugher.lfg.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bradflaugher.lfg.customtasks.common.CustomTaskDataForBuiltinTask
import com.bradflaugher.lfg.data.BuiltInTaskId
import com.bradflaugher.lfg.ui.modelmanager.ModelManager
import com.bradflaugher.lfg.ui.modelmanager.ModelManagerViewModel

private const val ROUTE_AGENT = "agent"
private const val ROUTE_MODEL_MANAGER = "model_manager"

@Composable
fun LfgNavHost(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()

  DisposableEffect(lifecycleOwner) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_START,
          Lifecycle.Event.ON_RESUME,
          -> modelManagerViewModel.setAppInForeground(foreground = true)
          Lifecycle.Event.ON_STOP,
          Lifecycle.Event.ON_PAUSE,
          -> modelManagerViewModel.setAppInForeground(foreground = false)
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  NavHost(
    navController = navController,
    startDestination = ROUTE_AGENT,
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    modifier = modifier,
  ) {
    composable(route = ROUTE_AGENT) {
      val agentTask = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_AGENT_CHAT)
      if (agentTask != null) {
        // Auto-select first available downloaded model for this task (or just the first listed
        // model if none downloaded — user will be prompted to download from the gear icon).
        LaunchedEffect(agentTask, modelManagerUiState.tasks) {
          if (agentTask.models.isNotEmpty()) {
            val selected = modelManagerUiState.selectedModel
            val belongsToTask = agentTask.models.any { it.name == selected.name }
            if (!belongsToTask) {
              modelManagerViewModel.selectModel(agentTask.models.first())
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          modelManagerViewModel.customTasks
            .firstOrNull { it.task.id == BuiltInTaskId.LLM_AGENT_CHAT }
            ?.MainScreen(
              CustomTaskDataForBuiltinTask(
                modelManagerViewModel = modelManagerViewModel,
                onNavUp = {},
                onSettingsClicked = { navController.navigate(ROUTE_MODEL_MANAGER) },
                initialQuery = null,
              ),
            )
        }
      }
    }

    composable(route = ROUTE_MODEL_MANAGER) {
      val agentTask = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_AGENT_CHAT)
      if (agentTask != null) {
        ModelManager(
          task = agentTask,
          viewModel = modelManagerViewModel,
          navigateUp = { navController.popBackStack() },
          onModelClicked = { model ->
            modelManagerViewModel.selectModel(model)
            navController.popBackStack()
          },
        )
      }
    }
  }
}

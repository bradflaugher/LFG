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

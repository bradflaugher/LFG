package com.bradflaugher.lfg.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.bradflaugher.lfg.proto.Theme

object ThemeSettings {
  val themeOverride = mutableStateOf<Theme>(Theme.THEME_AUTO)
}

/*
 * LFE — Low-Feature Edge agent. Apache 2.0.
 * Forked from Google AI Edge Gallery (Apache 2.0, Copyright 2025 Google LLC).
 */

package com.bradflaugher.lfe

import android.app.Application
import com.bradflaugher.lfe.data.DataStoreRepository
import com.bradflaugher.lfe.ui.theme.ThemeSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LfeApplication : Application() {

  @Inject lateinit var dataStoreRepository: DataStoreRepository

  override fun onCreate() {
    super.onCreate()
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()
  }
}

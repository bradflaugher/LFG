package com.bradflaugher.lfg

import android.app.Application
import com.bradflaugher.lfg.data.DataStoreRepository
import com.bradflaugher.lfg.ui.theme.ThemeSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LfgApplication : Application() {
  @Inject lateinit var dataStoreRepository: DataStoreRepository

  override fun onCreate() {
    super.onCreate()
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()
  }
}

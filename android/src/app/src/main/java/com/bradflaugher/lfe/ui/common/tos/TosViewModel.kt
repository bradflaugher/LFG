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
package com.bradflaugher.lfe.ui.common.tos

import androidx.lifecycle.ViewModel
import com.bradflaugher.lfe.data.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** ViewModel responsible for managing terms of services related tasks. */
@HiltViewModel
open class TosViewModel @Inject constructor(private val dataStoreRepository: DataStoreRepository) :
  ViewModel() {
  open fun getIsTosAccepted(): Boolean {
    return dataStoreRepository.isTosAccepted()
  }

  open fun acceptTos() {
    dataStoreRepository.acceptTos()
  }

  open fun getIsGemmaTermsOfUseAccepted(): Boolean {
    return dataStoreRepository.isGemmaTermsOfUseAccepted()
  }

  open fun acceptGemmaTermsOfUse() {
    dataStoreRepository.acceptGemmaTermsOfUse()
  }
}

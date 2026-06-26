package com.bradflaugher.lfg.ui.common.tos

import androidx.lifecycle.ViewModel
import com.bradflaugher.lfg.data.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** ViewModel responsible for managing terms of services related tasks. */
@HiltViewModel
open class TosViewModel
  @Inject
  constructor(private val dataStoreRepository: DataStoreRepository) :
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

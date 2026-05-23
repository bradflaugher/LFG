/*
 * LFE — A low-feature, on-device AI agent for Android.
 * Copyright (C) 2026 Brad Flaugher
 *
 * Licensed under the GNU General Public License v3.0 or later.
 * See LICENSE in the project root for terms.
 */
package com.bradflaugher.lfe.ui.modelmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bradflaugher.lfe.data.DataStoreRepository
import com.bradflaugher.lfe.data.HfModelInfo
import com.bradflaugher.lfe.data.HuggingFaceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * State for the HuggingFace browse screen.
 *
 * `liteRtOnly` is on by default — without it, results include `.safetensors` and `.gguf` repos
 * that LFE's LiteRT-LM runtime can't load.
 */
data class HfBrowseUiState(
  val query: String = "",
  val liteRtOnly: Boolean = true,
  val loading: Boolean = false,
  val loadingMore: Boolean = false,
  val error: String? = null,
  val results: List<HfModelInfo> = emptyList(),
  val nextPageUrl: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HfBrowseViewModel
@Inject
constructor(private val dataStoreRepository: DataStoreRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(HfBrowseUiState())
  val uiState = _uiState.asStateFlow()

  private val _query = MutableStateFlow("")
  private var activeJob: Job? = null

  init {
    // Debounce typing so we don't hit HF on every keystroke.
    _query
      .debounce(450)
      .distinctUntilChanged()
      .drop(1) // skip the initial empty value
      .onEach { fetch(it, append = false) }
      .launchIn(viewModelScope)

    // Kick off an initial browse so the screen isn't empty on open.
    fetch(query = "", append = false)
  }

  fun onQueryChange(query: String) {
    _uiState.update { it.copy(query = query) }
    _query.value = query
  }

  fun toggleLiteRtOnly() {
    _uiState.update { it.copy(liteRtOnly = !it.liteRtOnly) }
  }

  fun loadMore() {
    val state = _uiState.value
    val next = state.nextPageUrl ?: return
    if (state.loadingMore) return
    fetch(query = state.query, append = true, pageUrl = next)
  }

  fun retry() {
    fetch(query = _uiState.value.query, append = false)
  }

  private fun fetch(query: String, append: Boolean, pageUrl: String? = null) {
    activeJob?.cancel()
    activeJob =
      viewModelScope.launch(Dispatchers.IO) {
        _uiState.update {
          if (append) it.copy(loadingMore = true, error = null)
          else it.copy(loading = true, error = null)
        }
        val token = dataStoreRepository.readAccessTokenData()?.accessToken?.takeIf { it.isNotBlank() }
        // Default browse with no query: surface the litert-community org so first results are
        // immediately runnable. Once the user types, drop the author filter.
        val page =
          if (pageUrl != null) {
            HuggingFaceApi.searchModels(nextPageUrl = pageUrl, authToken = token)
          } else if (query.isBlank()) {
            HuggingFaceApi.searchModels(
              author = "litert-community",
              limit = 50,
              sort = "downloads",
              authToken = token,
            )
          } else {
            HuggingFaceApi.searchModels(
              search = query,
              limit = 25,
              sort = "downloads",
              authToken = token,
            )
          }

        if (page == null) {
          _uiState.update {
            it.copy(
              loading = false,
              loadingMore = false,
              error = "Couldn't reach HuggingFace. Check connection and try again.",
            )
          }
          return@launch
        }

        _uiState.update {
          val combined = if (append) it.results + page.models else page.models
          it.copy(
            loading = false,
            loadingMore = false,
            results = combined,
            nextPageUrl = page.nextLink,
          )
        }
      }
  }

  private fun MutableStateFlow<HfBrowseUiState>.update(
    fn: (HfBrowseUiState) -> HfBrowseUiState
  ) {
    value = fn(value)
  }
}

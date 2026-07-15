package com.neoruaa.xhsdn.viewmodels

import androidx.lifecycle.ViewModel
import com.neoruaa.xhsdn.DetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState(emptyList(), "", false))
    val state: StateFlow<DetailUiState> = _state

    fun updateState(newState: DetailUiState) {
        _state.value = newState
    }

    fun removeMediaItem(mediaItem: MediaItem) {
        val currentState = _state.value
        val updatedMediaItems = currentState.mediaItems.filter { it.path != mediaItem.path }
        _state.value = currentState.copy(mediaItems = updatedMediaItems)
    }
}
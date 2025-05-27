package com.aking.starter.screens.floating

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aking.starter.models.DropItem
import com.aking.starter.screens.floating.data.FloatingRepository
import com.aking.starter.screens.floating.drag.DragDropManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Created by Ak on 2025-05-25 21:18.
 */
class FloatingViewModel(
    private val repository: FloatingRepository = FloatingRepository(),
    private val dragDropManager: DragDropManager = DragDropManager()
) : ViewModel() {

    private val _uiState = MutableFloatingUiState()
    val uiState: FloatingUiState = _uiState

    private val _effects = Channel<FloatingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var dropSessionIdGen = 0L

    init {
        viewModelScope.launch {
            repository.items
                .onEach { items ->
                    Log.i("TAG", "onEach: ")
                    _uiState.items = items
                    computeGroupedItems(items)
                }
                .collect()
        }
    }

    private suspend fun computeGroupedItems(items: List<DropItem>) {
        withContext(Dispatchers.Default) {
            _uiState.groupedItems = items
                .groupBy { it.groupId }
                .toSortedMap(compareByDescending { it })
        }
    }

    private fun handleToggleGroup(groupId: Long) {
        _uiState.expandedGroups = if (groupId in _uiState.expandedGroups) {
            _uiState.expandedGroups - groupId
        } else {
            _uiState.expandedGroups + groupId
        }
    }

    fun reducer(intent: FloatingIntent) {
        when (intent) {
            is FloatingIntent.OnDrop -> handleDrop(intent.context, intent.event)
            is FloatingIntent.DropData -> handleDragData(intent.context, intent.items)
            is FloatingIntent.ClearAll -> handleClear()
            is FloatingIntent.RemoveItem -> handleRemove(intent.id)
            is FloatingIntent.ToggleGroupExpansion -> handleToggleGroup(intent.groupId)
        }
    }

    private fun handleDrop(context: Context, event: DragAndDropEvent) {
        Log.i("TAG", "handleDrop: ")
        viewModelScope.launch {
            _uiState.isLoading = true
            val currentDropSessionId = ++dropSessionIdGen
            var hasFailure = false

            dragDropManager.handleDrop(context, event).forEach { clipDataItem ->
                val result = when {
                    clipDataItem.text != null -> {
                        repository.addTextItem(clipDataItem.text.toString(), currentDropSessionId)
                    }
                    clipDataItem.uri != null -> {
                        val mimeType = context.contentResolver.getType(clipDataItem.uri)
                        if (mimeType != null && mimeType.isNotBlank()) {
                            repository.addMediaItem(clipDataItem.uri, mimeType, currentDropSessionId)
                        } else {
                            Result.failure(IllegalArgumentException("Cannot determine type of dropped file for URI: ${clipDataItem.uri}"))
                        }
                    }
                    else -> {
                        Result.failure(IllegalArgumentException("Unsupported item dropped: $clipDataItem"))
                    }
                }

                result.onFailure { e ->
                    hasFailure = true
                    _uiState.error = e.message ?: "Error processing dropped item"
                    _effects.send(FloatingEffect.ShowSnackbar(e.message ?: "Unknown error processing dropped item"))
                }
            }
            // Optionally, if any item in a drop session fails, you might want to notify the user about partial success
            if (hasFailure) {
                 _effects.send(FloatingEffect.ShowSnackbar("Some items could not be processed."))
            }
            _uiState.isLoading = false
        }
    }

    private fun handleDragData(context: Context, items: List<DropItem>) {
        viewModelScope.launch {
            _uiState.error = null
            kotlin.runCatching {
                dragDropManager.prepareDragData(context, items)
            }.onFailure { e ->
                _uiState.error = e.message
                _effects.send(FloatingEffect.ShowSnackbar(e.message ?: "Failed to prepare drag data"))
            }
        }
    }

    private fun handleClear() {
        viewModelScope.launch {
            _uiState.error = null
            repository.clear()
                .onFailure { e ->
                    _uiState.error = e.message
                    _effects.send(FloatingEffect.ShowSnackbar(e.message ?: "Failed to clear items"))
                }
        }
    }

    private fun handleRemove(id: Long) {
        viewModelScope.launch {
            _uiState.error = null
            repository.removeItem(id)
                .onFailure { e ->
                    _uiState.error = e.message
                    _effects.send(FloatingEffect.ShowSnackbar(e.message ?: "Failed to remove item"))
                }
        }
    }
}


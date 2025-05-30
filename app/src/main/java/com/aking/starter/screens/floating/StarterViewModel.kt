package com.aking.starter.screens.floating

import android.content.ContentResolver
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aking.starter.screens.floating.data.DropItem
import com.aking.starter.screens.floating.data.FloatingRepository
import com.aking.starter.screens.floating.drag.DragDropManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author Created by Ak on 2025-05-25 21:18.
 */
class FloatingViewModel(
    private val repository: FloatingRepository = FloatingRepository(),
    private val dragDropManager: DragDropManager = DragDropManager()
) : ViewModel() {

    // UI State
    private val _uiState = MutableFloatingUiState()
    val uiState: FloatingUiState = _uiState

    // 定义一个 Channel 用于发送 Effect
    private val _effectChannel = Channel<FloatingEffect>(Channel.BUFFERED)
    val effectFlow = _effectChannel.receiveAsFlow()

    // 触发 Effect
    private fun effect(effect: FloatingEffect) {
        viewModelScope.launch {
            _effectChannel.send(effect)
        }
    }
    
    // 缓存 lambda 避免重复创建
    private val itemsCollector: suspend (List<DropItem>) -> Unit = { items -> _uiState.items = items }

    init {
        repository.items
            .onEach(itemsCollector)
            .launchIn(viewModelScope)
    }

    fun reducer(intent: FloatingIntent) {
        intent.run {
            when (this) {
                is FloatingIntent.OnDrop -> handleDrop(contentResolver, event)
                is FloatingIntent.DropData -> handleDragData(items)
                is FloatingIntent.ClearAll -> handleClearAll()
                is FloatingIntent.RemoveItem -> handleRemoveItem(id)
            }
        }
    }

    private fun handleDrop(contentResolver: ContentResolver, event: DragAndDropEvent) {
        launchWithLoading {
            dragDropManager.handleDrop(event).forEach { clipDataItem ->
                when {
                    clipDataItem.text != null -> {
                        repository.addTextItem(clipDataItem.text.toString())
                    }
                    clipDataItem.uri != null -> {
                        contentResolver.getType(clipDataItem.uri)?.takeIf { it.isNotBlank() }?.let { mimeType ->
                            repository.addMediaItem(clipDataItem.uri, mimeType)
                        } ?: effect(FloatingEffect.ShowSnackbar("Unsupported item dropped"))
                    }
                    else -> effect(FloatingEffect.ShowSnackbar("Unsupported item dropped"))
                }
            }
        }
    }

    private fun handleDragData(items: List<DropItem>) {
        dragDropManager.prepareDragData(items)
    }

    private fun handleClearAll() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    private fun handleRemoveItem(id: Long) {
        viewModelScope.launch {
            repository.removeItem(id)
        }
    }

    private fun launchWithLoading(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(context) {
            try {
                _uiState.isLoading = true
                block()
            } finally {
                _uiState.isLoading = false
            }
        }
    }
}


package com.aking.starter.screens.floating

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.util.Log
import android.view.View.DRAG_FLAG_GLOBAL
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.core.app.ActivityCompat.requestDragAndDropPermissions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aking.starter.models.DropItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * @author Created by Ak on 2025-05-25 21:18.
 */
class StarterViewModel : ViewModel() {
    var itemIdGen = 0L

    // UI State
    private val _uiState = MutableFloatingUiState()
    val uiState: FloatingUiState = _uiState

    // 定义一个 Channel 用于发送 Effect
    private val _effectChannel = Channel<FloatingEffect>(Channel.BUFFERED)
    val effectFlow = _effectChannel.receiveAsFlow()

    fun reducer(intent: FloatingIntent) {
        intent.run {
            when (this) {
                is FloatingIntent.OnDrop -> handleDropItem(act, event)
                is FloatingIntent.DropData -> handleDropData(content, item)
            }
        }
    }

    // 触发 Effect
    fun effect(effect: FloatingEffect) {
        viewModelScope.launch {
            _effectChannel.send(effect)
        }
    }

    private fun handleDropItem(context: Context, event: DragAndDropEvent) {
        val dragEvent = event.toAndroidDragEvent()
        Log.i("StarterViewModel", "handleDropItem:${dragEvent.clipData.itemCount} ${event.mimeTypes()}")
        requestDragAndDropPermissions(context as Activity, dragEvent)
        // 解析拖入的数据
        dragEvent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)
                when {
                    item.uri != null -> {
                        val uri = item.uri
                        val mime = context.contentResolver.getType(uri) ?: ""
                        if (mime.startsWith("image/")) {
                            _uiState.dropList.add(DropItem.Image(uri, ++itemIdGen))
                        } else {
                            _uiState.dropList.add(DropItem.FileItem(uri, ++itemIdGen))
                        }
                    }

                    item.text != null -> {
                        _uiState.dropList.add(DropItem.Text(item.text.toString(), ++itemIdGen))
                    }
                }
            }
        }
    }

    /** 发起拖拽分享 */
    private fun handleDropData(context: Context, item: DropItem) {
        val dragData = when (item) {
            is DropItem.Text -> ClipData.newPlainText("text", item.text)
            is DropItem.Image, is DropItem.FileItem -> {
                val uri = when (item) {
                    is DropItem.Image -> item.uri
                    is DropItem.FileItem -> item.uri
                    else -> null
                }
                ClipData.newUri(context.contentResolver, "file", uri)
            }
        }
        DragAndDropTransferData(
            dragData,
            flags = DRAG_FLAG_GLOBAL
        )
    }
}


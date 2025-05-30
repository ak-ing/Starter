package com.aking.starter.screens.floating.drag

import android.content.ClipData
import android.view.View
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import com.aking.starter.screens.floating.data.DropItem

class DragDropManager {
    fun handleDrop(event: DragAndDropEvent): List<ClipData.Item> {
        val dragEvent = event.toAndroidDragEvent()
        return dragEvent.clipData?.let { clipData ->
            (0 until clipData.itemCount).map { clipData.getItemAt(it) }
        } ?: emptyList()
    }

    fun prepareDragData(items: List<DropItem>): DragAndDropTransferData {
        val clipData = ClipData.newPlainText("", "") // 创建一个空的 ClipData 作为容器
        items.forEach { item ->
            when (item) {
                is DropItem.Text -> clipData.addItem(ClipData.Item(item.text))
                is DropItem.Image -> clipData.addItem(ClipData.Item(item.uri))
                is DropItem.FileItem -> clipData.addItem(ClipData.Item(item.uri))
            }
        }
        return DragAndDropTransferData(clipData, flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ)
    }
} 
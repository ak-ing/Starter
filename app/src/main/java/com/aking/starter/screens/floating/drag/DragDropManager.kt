package com.aking.starter.screens.floating.drag

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.view.View
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.core.app.ActivityCompat
import com.aking.starter.models.DropItem

class DragDropManager {
    fun handleDrop(context: Context, event: DragAndDropEvent): List<ClipData.Item> {
        val dragEvent = event.toAndroidDragEvent()
        ActivityCompat.requestDragAndDropPermissions(context as Activity, dragEvent)
        return dragEvent.clipData?.let { clipData ->
            (0 until clipData.itemCount).map { clipData.getItemAt(it) }
        } ?: emptyList()
    }

    fun prepareDragData(context: Context, items: List<DropItem>): DragAndDropTransferData {
        val clipData = ClipData.newPlainText("", "") // 创建一个空的 ClipData 作为容器
        
        items.forEach { item ->
            when (item) {
                is DropItem.Text -> {
                    clipData.addItem(ClipData.Item(item.text))
                }
                is DropItem.Image, is DropItem.FileItem -> {
                    val uri = when (item) {
                        is DropItem.Image -> item.uri
                        is DropItem.FileItem -> item.uri
                        else -> null
                    }
                    if (uri != null) {
                        clipData.addItem(ClipData.Item(uri))
                    }
                }
            }
        }

        return DragAndDropTransferData(
            clipData,
            flags = View.DRAG_FLAG_GLOBAL
        )
    }
} 
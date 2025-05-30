package com.aking.starter.screens.floating.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository负责数据操作，不持有状态
 */
class FloatingRepository {
    private var itemIdGen = 0L
    val items = MutableStateFlow<List<DropItem>>(emptyList())

    suspend fun addTextItem(text: String): Result<Unit> = runCatching {
        items.update { currentItems ->
            currentItems + DropItem.Text(text, ++itemIdGen)
        }
    }

    suspend fun addMediaItem(uri: Uri, mime: String): Result<Unit> = runCatching {
        val item = if (mime.startsWith("image/")) {
            DropItem.Image(uri, ++itemIdGen)
        } else {
            DropItem.FileItem(uri, ++itemIdGen)
        }
        items.update { currentItems -> currentItems + item }
    }

    suspend fun removeItem(id: Long): Result<Unit> = runCatching {
        items.update { currentItems ->
            currentItems.filterNot { it.id == id }
        }
    }

    suspend fun clear(): Result<Unit> = runCatching {
        items.update { emptyList() }
    }
} 
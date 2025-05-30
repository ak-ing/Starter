package com.aking.starter.screens.floating.data

import android.net.Uri

/**
 * 数据模型，支持多类型
 * @author Created by Ak on 2025-05-25 20:11.
 */
sealed class DropItem(val id: Long) {
    data class Text(val text: String, val itemId: Long) : DropItem(itemId)
    data class Image(val uri: Uri, val itemId: Long) : DropItem(itemId)
    data class FileItem(val uri: Uri, val itemId: Long) : DropItem(itemId)
}
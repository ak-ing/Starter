package com.aking.starter.models

import android.net.Uri

/**
 * 数据模型，支持多类型
 * @author Created by Ak on 2025-05-25 20:11.
 */
sealed class DropItem(val id: Long, val groupId: Long) {
    data class Text(val text: String, val itemId: Long, val itemGroupId: Long) : DropItem(itemId, itemGroupId)
    data class Image(val uri: Uri, val itemId: Long, val itemGroupId: Long) : DropItem(itemId, itemGroupId)
    data class FileItem(val uri: Uri, val itemId: Long, val itemGroupId: Long) : DropItem(itemId, itemGroupId)
}
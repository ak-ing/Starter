package com.aking.starter.screens.floating

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draganddrop.DragAndDropEvent
import com.aking.starter.models.DropItem

// UI状态
@Stable
interface FloatingUiState {
    val dropList: List<DropItem>
}

class MutableFloatingUiState : FloatingUiState {
    override val dropList = mutableStateListOf<DropItem>()
}

// 用户意图
sealed interface FloatingIntent {
    data class OnDrop(val act: Context, val event: DragAndDropEvent) : FloatingIntent
    data class DropData(val content: Context, val item: DropItem) : FloatingIntent
}

// 副作用（可选）
sealed interface FloatingEffect {
    data class ShowMessage(val msg: String) : FloatingEffect
}


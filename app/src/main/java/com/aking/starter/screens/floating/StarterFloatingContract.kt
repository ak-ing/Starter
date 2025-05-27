package com.aking.starter.screens.floating

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draganddrop.DragAndDropEvent
import com.aking.starter.models.DropItem

@Stable
interface FloatingUiState {
    val items: List<DropItem>
    val groupedItems: Map<Long, List<DropItem>>
    val isLoading: Boolean
    val error: String?
    val expandedGroups: Set<Long>
}

internal class MutableFloatingUiState : FloatingUiState {
    override var items: List<DropItem> by mutableStateOf(emptyList())
    override var groupedItems: Map<Long, List<DropItem>> by mutableStateOf(emptyMap())
    override var isLoading: Boolean by mutableStateOf(false)
    override var error: String? by mutableStateOf(null)
    override var expandedGroups: Set<Long> by mutableStateOf(emptySet())
}

sealed interface FloatingIntent {
    data class OnDrop(val context: Context, val event: DragAndDropEvent) : FloatingIntent
    data class DropData(val context: Context, val items: List<DropItem>) : FloatingIntent
    data object ClearAll : FloatingIntent
    data class RemoveItem(val id: Long) : FloatingIntent
    data class ToggleGroupExpansion(val groupId: Long) : FloatingIntent
}

sealed interface FloatingEffect {
    data class ShowSnackbar(val message: String) : FloatingEffect
}


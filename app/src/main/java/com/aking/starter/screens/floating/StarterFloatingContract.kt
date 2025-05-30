package com.aking.starter.screens.floating

import android.content.ContentResolver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draganddrop.DragAndDropEvent
import com.aking.starter.screens.floating.data.DropItem

@Stable
interface FloatingUiState {
    val items: List<DropItem>
    val isLoading: Boolean
    val error: String?
}

internal class MutableFloatingUiState : FloatingUiState {
    override var items: List<DropItem> by mutableStateOf(emptyList())
    override var isLoading: Boolean by mutableStateOf(false)
    override var error: String? by mutableStateOf(null)
}

sealed interface FloatingIntent {
    data class OnDrop(val contentResolver: ContentResolver, val event: DragAndDropEvent) : FloatingIntent
    data class DropData(val items: List<DropItem>) : FloatingIntent
    data object ClearAll : FloatingIntent
    data class RemoveItem(val id: Long) : FloatingIntent
}

sealed interface FloatingEffect {
    data class ShowSnackbar(val message: String) : FloatingEffect
}


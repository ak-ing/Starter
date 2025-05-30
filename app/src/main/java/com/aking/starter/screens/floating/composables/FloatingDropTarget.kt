package com.aking.starter.screens.floating.composables

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget

@Composable
fun FloatingDropTarget(
    modifier: Modifier = Modifier,
    onStarted: (DragAndDropEvent) -> Unit = {},
    onDrop: (DragAndDropEvent) -> Unit,
    content: @Composable () -> Unit
) {
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) = onStarted(event)
            override fun onEnded(event: DragAndDropEvent) {}
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDrop(event)
                return true
            }
        }
    }
    Box(
        modifier = modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dropTarget
        )
    ) {
        content()
    }
} 
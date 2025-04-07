package com.aking.starter.screens.floating

import android.content.Context
import android.view.Gravity
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.views.BaseFloatingComposeView

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override fun shrinkToEdge(): Boolean = true     // 是否支持在屏幕边缘折叠

    @Composable
    override fun FloatingContent() {
        val viewConfiguration = remember { ViewConfiguration.get(context) }
        AnimatedContent(edgeState) {
            if (it) {
                var delta = remember { 0 }
                Box(
                    Modifier
                        .width(with(LocalDensity.current) { viewConfiguration.scaledEdgeSlop.toDp() })
                        .height(100.dp)
                        .background(Color.White.copy(alpha = 0.5f))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            startDragImmediately = true,
                            state = rememberDraggableState {
                                delta += it.toInt()
                                if (direction == Gravity.START && delta >= 0) {
                                    expand()
                                } else if (direction == Gravity.END && delta <= 0) {
                                    expand()
                                }
                            },
                            onDragStarted = { delta = 0 })
                )
            } else {
                Column {
                    var text by remember { mutableStateOf("text") }
                    Text(
                        text, Modifier
                            .size(100.dp)
                            .background(Color.Gray)
                            .clickable {
                                text = "啊啊啊啊啊啊"
                            })
                }
            }
        }
//        AnimatedVisibility(edgeState) {
//
//        }
//
//        AnimatedVisibility(!edgeState) {
//
//        }
    }
}
package com.aking.starter.screens.floating

import android.content.Context
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.runtime.mutableIntStateOf
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

    @Composable
    override fun Content() {
        val viewConfiguration = remember { ViewConfiguration.get(context) }
        var text by remember { mutableStateOf("text") }
        var width by remember { mutableIntStateOf(viewConfiguration.scaledEdgeSlop) }
        val animEdgeWidth by animateIntAsState(width)

        AnimatedContent(edgeState) {
            if (it) {
                Box(
                    Modifier
                        .width(with(LocalDensity.current) { animEdgeWidth.toDp() })
                        .height(100.dp)
                        .background(Color.Blue)
                        .clickable { edgeState = false }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { width = (width + it.toInt()).coerceAtLeast(0) },
                            onDragStopped = {
                                if (width >= viewConfiguration.scaledEdgeSlop * 2) {
                                    edgeState = false
                                } else {
                                    edgeState = true
                                }
                                width = viewConfiguration.scaledEdgeSlop
                            }
                        )
                )
            } else {
                Column {
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
    }
}

/**
 * 悬浮窗状态
 */
enum class FloatState {
    EDGE, EXPAND, DRAG
}
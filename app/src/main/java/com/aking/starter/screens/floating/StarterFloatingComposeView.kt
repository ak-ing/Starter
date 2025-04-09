package com.aking.starter.screens.floating

import android.content.Context
import android.view.Gravity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.views.BaseFloatingComposeView
import com.aking.starter.utils.LocalAndroidViewConfiguration

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override fun shrinkToEdge(): Boolean = true     // 是否支持在屏幕边缘折叠

    @Composable
    override fun FloatingContent() {
        val alignment by remember {
            derivedStateOf {
                if (direction == Gravity.START) Alignment.TopStart else Alignment.TopEnd
            }
        }
        AnimatedContent(edgeState, contentAlignment = alignment) {
            if (it) {
                EdgeBar {
                    if (direction == Gravity.START && it >= 0) {
                        expand()
                    } else if (direction == Gravity.END && it <= 0) {
                        expand()
                    }
                }
            } else {
                var currentPanel by remember { mutableIntStateOf(0) }
                AnimatedContent(currentPanel) {
                    when (it) {
                        0 -> TransferPanel()
                        1 -> Text(text = "我的", modifier = Modifier.clickable { currentPanel = 0 })
                    }
                }
            }
        }
//        Box(contentAlignment = alignment) {
//            // 屏幕边缘折叠
//            AnimatedVisibility(edgeState, enter = fadeIn() + scaleIn(), exit = scaleOut() + fadeOut()) {
//
//            }
//            // 内容
//            AnimatedVisibility(!edgeState, enter = fadeIn() + scaleIn(), exit = scaleOut() + fadeOut()) {
//
//            }
//        }
    }

}

/**
 * 屏幕边缘折叠
 */
@Composable
fun EdgeBar(onDrag: (Float) -> Unit) {
    val density = LocalDensity.current
    val viewConfiguration = LocalAndroidViewConfiguration.current
    val width = remember { with(density) { viewConfiguration.scaledEdgeSlop.toDp() } }
    var delta = remember { 0f }
    Box(
        modifier = Modifier
            .width(width)
            .height(100.dp)
            .background(Background, shape = RoundedCornerShape(percent = 50))
            .draggable(
                orientation = Orientation.Horizontal,
                startDragImmediately = true,
                state = rememberDraggableState {
                    delta += it
                    onDrag(delta)
                },
                onDragStarted = { delta = 0f })
    )
}

@Preview
@Composable
fun EdgeFloatingPreview() {
    EdgeBar { }
}
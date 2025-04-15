package com.aking.starter.screens.floating

import android.content.Context
import android.view.Gravity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.ColorEdgeEdit
import com.aking.starter.ui.views.BaseFloatingComposeView
import com.aking.starter.utils.LocalAndroidViewConfiguration
import kotlinx.coroutines.launch

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override fun shrinkToEdge(): Boolean = true     // 是否支持在屏幕边缘折叠

    @Composable
    override fun FloatingContent() {
        val scope = rememberCoroutineScope()
        Box {
            var deltaX by remember { mutableFloatStateOf(0f) }
            val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
            var scrolled by remember { mutableStateOf(false) }
            val offsetAnima by animateIntOffsetAsState(if (scrolled) {
                IntOffset(-deltaX.toInt(), 0)
            } else IntOffset.Zero)
            // 屏幕边缘折叠
            EdgeBar(
                modifier = Modifier
                    .offset { offsetAnima }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            scope.launch {
                                interactionSource.emit(pressInteraction)
                                deltaX = 0f
                            }
                        }, onDragStopped = {
                            scope.launch {
                                interactionSource.emit(PressInteraction.Release(pressInteraction))
                                scrolled = false
                            }
                        },
                        state = rememberDraggableState {
                            deltaX += it
                            if (direction == Gravity.START && deltaX >= touchSlop) {
                                scrolled = true
                            } else if (direction == Gravity.END && deltaX <= -touchSlop) {
                                scrolled = true
                            }
                        }
                    ))

            // 内容
            AnimatedVisibility(!edgeState, enter = fadeIn() + scaleIn(), exit = scaleOut() + fadeOut()) {
                TransferPanel()
            }
        }
    }

    /**
     * 屏幕边缘折叠
     */
    @Composable
    fun EdgeBar(modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        val viewConfiguration = LocalAndroidViewConfiguration.current
        // 折叠条交互
        val widthAnima by animateDpAsState(with(density) {
            (if (interactions.isNotEmpty()) viewConfiguration.scaledEdgeSlop else viewConfiguration.scaledEdgeSlop / 2).toDp()
        })
        val draggedState by interactionSource.collectIsDraggedAsState()

        Box(
            modifier = modifier
                .width(widthAnima)
                .height(100.dp)
                .background(if (draggedState) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}



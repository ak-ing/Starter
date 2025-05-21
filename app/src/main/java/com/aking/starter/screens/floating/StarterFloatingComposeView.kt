package com.aking.starter.screens.floating

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.ColorEdgeEdit
import com.aking.starter.ui.views.BaseFloatingComposeView
import com.aking.starter.utils.dpValue
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override suspend fun handlerEdgeState() {
        if (edgeState) { // Shrink
            deltaX = if (isLeft) -1f else 1f // Ensure it's minimal to hide
            dragTo(isRelease = true)
        } else { // Expand
            deltaX = if (isLeft) contentWidthPx else -contentWidthPx
            dragTo(isRelease = true)
        }
    }

    override fun handleGlobalDragStart() {
        if (edgeState) { // Only expand if currently shrunk/docked
            lifecycle.coroutineScope.launch {
                expandView()
            }
        }
    }

    // 记住交互事件（松手时需要移除）
    private val pressInteraction = PressInteraction.Press(Offset.Zero)
    // Adjusted for TransferPanel (100.dp) + padding (edgeSlop * 2)
    private val contentWidthPx = 100.dpValue + edgeSlop * 2

    // 滑动距离
    private var deltaX = 0f

    // 内容是否显示
    var visibleContent = false

    // EdgeBar偏移动画
    val edgeOffset = Animatable(0f)

    //内容偏移动画
    val animOffset = Animatable(if (isLeft) -contentWidthPx else contentWidthPx)


    @Composable
    override fun FloatingContent() {
        Box {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            // 拖动状态（长按拖拽）
            val dragState by interactionSource.collectIsDraggedAsState()

            LaunchedEffect(direction, contentWidthPx) { // Add contentWidthPx dependency
                if (isLeft) {
                    animOffset.snapTo(-contentWidthPx)
                } else {
                    animOffset.snapTo(contentWidthPx)
                }
            }

            // 屏幕边缘折叠条
            EdgeBar(
                enableDrag = dragState,
                Modifier
                    .graphicsLayer {
                        translationX = edgeOffset.value
                        alpha = 1f - edgeOffset.value.absoluteValue / edgeSlop.toFloat()
                    }
                    .draggable(
                        enabled = !dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            scope.launch {
                                interactionSource.emit(pressInteraction)
                            }
                        }, onDragStopped = {
                            scope.launch {
                                interactionSource.emit(PressInteraction.Release(pressInteraction))
                                if (deltaX.absoluteValue >= contentWidthPx / 2) {
                                    expandView() // Changed from expand() to expandView() to avoid conflict
                                } else {
                                    shrinkView() // Changed from shrink() to shrinkView()
                                }
                            }
                        },
                        state = rememberDraggableState {
                            scope.launch { dragTo(delta = it) }
                        }
                    ))

            // 内容
            TransferPanel(
                modifier = Modifier
                    // 通过 layout 动态裁剪布局边界
                    .layout { measurable, constraints ->
                        // 计算实际可见区域
                        visibleContent = if (isLeft) {
                            (contentWidthPx + animOffset.value) > 0 && !dragState
                        } else {
                            (contentWidthPx - animOffset.value) > 0 && !dragState
                        }
                        val placeable = measurable.measure(constraints)
                        layout(if (visibleContent) placeable.width else 0, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .graphicsLayer {
                        translationX = animOffset.value
                        alpha = 1f - (animOffset.value.absoluteValue / contentWidthPx)
                    }
            )
        }
    }

    // Public method to expand the view
    suspend fun expandView() {
        _edgeState = false // Ensure edgeState is updated
        // Determine target deltaX to fully expand
        deltaX = if (isLeft) contentWidthPx else -contentWidthPx
        dragTo(isRelease = true)
    }

    // Public method to shrink the view
    suspend fun shrinkView() {
        _edgeState = true // Ensure edgeState is updated
        deltaX = if (isLeft) -1f else 1f // Minimal delta to hide
        dragTo(isRelease = true)
    }


    private suspend fun dragTo(delta: Float = 0f, isRelease: Boolean = false) {
        deltaX = if (isRelease) {
            if (deltaX.absoluteValue >= contentWidthPx / 2) {
                if (isLeft) {
                    contentWidthPx
                } else {
                    -contentWidthPx
                }
            } else {
                0f
            }
        } else {
            deltaX + delta
        }

        var runEdge = isRelease
        var runContent = isRelease
        if (isLeft && deltaX >= 0) {
            if (deltaX - delta <= edgeSlop) {
                runEdge = true
            } else {
                runContent = true
            }
        } else if (!isLeft && deltaX <= 0) {
            if (deltaX - delta >= -edgeSlop && !visibleContent) {
                runEdge = true
            } else {
                runContent = true
            }
        }

        val animas = mutableListOf<suspend () -> Unit>()
        if (runEdge) {
            animas.add {
                val edgeTarget = if (isLeft) {
                    -deltaX.coerceIn(0f, edgeSlop.toFloat())
                } else {
                    (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                }
                edgeOffset.animateTo(edgeTarget)
            }
        }
        if (runContent) {
            animas.add {
                val target = if (isLeft) {
                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                } else {
                    (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                }
                if (isRelease) {
                    animOffset.animateTo(target, tween())
                } else {
                    animOffset.animateTo(target)
                }
            }
        }
        if (isRelease) {
            animas.reverse()
        }
        animas.forEach { it() }
    }

    /**
     * 屏幕边缘折叠
     */
    @Composable
    fun EdgeBar(enableDrag: Boolean, modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        // 折叠条交互动画（touch变宽）
        val widthAnima by animateDpAsState(
            targetValue = with(density) { (if (interactions.isEmpty()) edgeSlop else edgeSlop * 2).toDp() },
            label = "edgeWidthAnimation"
        )
        val padding = remember { with(density) { (edgeSlop * 3).toDp() } }

        Box(
            modifier = modifier
                .width(widthAnima + padding)
                .height(100.dp) // Match TransferPanel height
                .systemGestureExclusion()
                .padding(start = if (isLeft) 0.dp else padding, end = if (isLeft) padding else 0.dp)
                .background(if (enableDrag) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}



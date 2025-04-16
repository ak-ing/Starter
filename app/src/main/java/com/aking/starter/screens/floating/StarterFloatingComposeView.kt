package com.aking.starter.screens.floating

import android.content.Context
import android.util.Log
import android.view.Gravity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aking.starter.ui.theme.Background
import com.aking.starter.ui.theme.ColorEdgeEdit
import com.aking.starter.ui.views.BaseFloatingComposeView
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override fun shrinkToEdge(): Boolean = true     // 是否支持在屏幕边缘折叠

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun FloatingContent() {
        Box {
            // 屏幕边缘折叠
            EdgeBar()

            // 内容
            val pagerState = rememberPagerState(pageCount = { 4 })
            val density = LocalDensity.current
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(with(density) { edgeSlop.toDp() })
                    .width(150.dp)
            )
            { page ->
                Card(
                    Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            // Calculate the absolute offset for the current page from the
                            // scroll position. We use the absolute value which allows us to mirror
                            // any effects for both directions
                            Log.i(
                                "TAG",
                                "FloatingContent: ${pagerState.currentPage}  $page   ${pagerState.currentPageOffsetFraction}"
                            )
                            val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState
                                        .currentPageOffsetFraction
                                    ).absoluteValue

                            // We animate the alpha, between 50% and 100%
                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        }
                ) {
                    // Card content
                    Box(
                        Modifier
                            .size(200.dp)
                            .background(Color.DarkGray)
                    ) { }
                }
            }
        }
    }

    /**
     * 屏幕边缘折叠
     */
    @Composable
    fun EdgeBar(modifier: Modifier = Modifier) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        // 折叠条交互动画（touch变宽）
        val widthAnima by animateDpAsState(with(density) {
            (if (interactions.isNotEmpty()) edgeSlop else edgeSlop / 2).toDp()
        })
        // 拖动状态（长按拖拽）
        val dragState by interactionSource.collectIsDraggedAsState()
        // 滑动状态（位移）
        var scrolling by remember { mutableStateOf(false) }

        // 滑动距离
        var deltaX by remember { mutableFloatStateOf(0f) }
        // 处理转换后的 Edge 偏移
        val edgeOffset by remember {
            derivedStateOf {
                if (!scrolling) {
                    0
                } else if (direction == Gravity.START) {
                    deltaX.toInt().inv().coerceIn(-edgeSlop, 0)
                } else {
                    deltaX.toInt().inv().coerceIn(0, edgeSlop)
                }
            }
        }
        // EdgeBar过渡动画,多个属性数值绑定到一个状态
        val transition = updateTransition(edgeOffset, label = "edgeBarTransition")
        // 偏移过渡
        val edgeBarOffset by transition.animateIntOffset { IntOffset(it, 0) }
        // 透明度过渡
        val edgeBarAlpha by transition.animateFloat { 1f - it.absoluteValue / edgeSlop.toFloat() }

        // 记住交互事件（松手时需要移除）
        val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
        Box(
            modifier = modifier
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
                            scrolling = false
                            deltaX = 0f
                        }
                    },
                    state = rememberDraggableState {
                        deltaX += it
                        if (direction == Gravity.START && deltaX >= touchSlop) {
                            scrolling = true
                        } else if (direction == Gravity.END && deltaX <= -touchSlop) {
                            scrolling = true
                        } else {
                            scrolling = false
                        }
                    }
                )
                .width(widthAnima)
                .height(100.dp)
                .offset { edgeBarOffset }
                .alpha(edgeBarAlpha)
                .background(if (dragState) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}



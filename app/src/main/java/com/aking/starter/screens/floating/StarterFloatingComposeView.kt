package com.aking.starter.screens.floating

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
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
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val contentWidthPx = with(density) { 150.dp.toPx() + edgeSlop * 2 }
            // 记住交互事件（松手时需要移除）
            val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
            // 拖动状态（长按拖拽）
            val dragState by interactionSource.collectIsDraggedAsState()

            // 内容是否显示
            var visibleContent = remember { false }
            // 滑动距离
            var deltaX = remember { 0f }
            // EdgeBar偏移动画
            val edgeOffset = remember { Animatable(0f) }
            //内容偏移动画
            val animOffset = remember { Animatable(-contentWidthPx) }

            LaunchedEffect(direction) {
                if (isLeft) {
                    animOffset.snapTo(-contentWidthPx)
                } else {
                    animOffset.snapTo(contentWidthPx)
                }
            }
            LaunchedEffect(edgeState) {
                if (!edgeState) return@LaunchedEffect
                deltaX = 0f
                val target = if (isLeft) {
                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                } else {
                    (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                }
                animOffset.animateTo(target, tween())
                edgeOffset.animateTo(0f, tween())
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
                                deltaX = if (deltaX.absoluteValue >= contentWidthPx / 2) {
                                    expand()
                                    if (isLeft) {
                                        contentWidthPx
                                    } else {
                                        -contentWidthPx
                                    }
                                } else {
                                    0f
                                }

                                val target = if (isLeft) {
                                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                                } else {
                                    (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                                }
                                animOffset.animateTo(target, tween())
                                val edgeTarget = if (isLeft) {
                                    -deltaX.coerceIn(0f, edgeSlop.toFloat())
                                } else {
                                    (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                                }
                                edgeOffset.animateTo(edgeTarget, tween())
                            }
                        },
                        state = rememberDraggableState {
                            scope.launch {
                                deltaX += it
                                var runEdge = false
                                var runContent = false
                                if (isLeft && deltaX >= 0) {
                                    if (deltaX - it <= edgeSlop) {
                                        runEdge = true
                                    } else {
                                        runContent = true
                                    }
                                } else if (!isLeft && deltaX <= 0) {
                                    if (deltaX - it >= -edgeSlop && !visibleContent) {
                                        runEdge = true
                                    } else {
                                        runContent = true
                                    }
                                }

                                if (runEdge) {
                                    val edgeTarget = if (isLeft) {
                                        -deltaX.coerceIn(0f, edgeSlop.toFloat())
                                    } else {
                                        (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                                    }
                                    edgeOffset.animateTo(edgeTarget)
                                }
                                if (runContent) {
                                    val target = if (isLeft) {
                                        (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                                    } else {
                                        (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                                    }
                                    animOffset.animateTo(target)
                                }
                            }
                        }
                    ))

            // 内容
            val pagerState = rememberPagerState(pageCount = { 4 })
            val measureResults: MutableMap<Boolean, MeasureResult> = remember { mutableMapOf() }
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    // 通过 layout 动态裁剪布局边界
                    .layout { measurable, constraints ->
                        // 计算实际可见区域
                        visibleContent = if (isLeft) {
                            (contentWidthPx + animOffset.value) > 0 && !dragAnimState
                        } else {
                            (contentWidthPx - animOffset.value) > 0 && !dragAnimState
                        }
                        measureResults.getOrPut(visibleContent) {
                            val placeable = measurable.measure(constraints)
                            layout(if (visibleContent) placeable.width else 0, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                    }
                    .graphicsLayer {
                        translationX = animOffset.value
                        alpha = 1f - (animOffset.value.absoluteValue / contentWidthPx)
//                        Log.i("TAG", "FloatingContent: contentWidthPx $contentWidthPx  ${animOffset.value}")
                    }
                    .padding(horizontal = with(density) { edgeSlop.toDp() })
                    .width(150.dp)

            ) { page ->
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
    fun EdgeBar(enableDrag: Boolean, modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        // 折叠条交互动画（touch变宽）
        val widthAnima by animateDpAsState(with(density) {
            (if (interactions.isEmpty()) edgeSlop else edgeSlop * 2).toDp()
        })
        val padding = remember { with(density) { (edgeSlop * 3).toDp() } }

        Box(
            modifier = modifier
                .width(widthAnima + padding)
                .height(100.dp)
                .systemGestureExclusion()
                .padding(start = if (isLeft) 0.dp else padding, end = if (isLeft) padding else 0.dp)
                .background(if (enableDrag) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}



package com.aking.starter.screens.floating

import android.content.Context
import android.util.Log
import android.view.Gravity
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
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
        val alignment by remember {
            derivedStateOf {
                if (direction == Gravity.START) Alignment.TopStart else Alignment.TopEnd
            }
        }
        Box(contentAlignment = alignment) {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val contentWidthPx = with(density) { 150.dp.toPx() + edgeSlop * 2 }
            // 记住交互事件（松手时需要移除）
            val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
            // 拖动状态（长按拖拽）
            val dragState by interactionSource.collectIsDraggedAsState()

            // 滑动距离
            var deltaX = remember { 0f }
            // EdgeBar偏移动画
            val edgeOffset = remember { Animatable(0f) }
            //内容偏移动画
            val animOffset = remember { Animatable(-contentWidthPx) }

            Log.i("TAG", "FloatingContent: recompose")

//            LaunchedEffect(direction) {
//                if (direction == Gravity.START) {
//                    animOffset.snapTo(-contentWidthPx)
//                } else {
//                    animOffset.snapTo(contentWidthPx)
//                }
//            }

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
                                    if (direction == Gravity.START) {
                                        contentWidthPx
                                    } else {
                                        -contentWidthPx
                                    }
                                } else {
                                    0f
                                }
                                Log.i("TAG", "FloatingContentaaa: onDragStopped $deltaX")

                                val target = if (direction == Gravity.START)
                                    (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                                else (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                                animOffset.animateTo(target, tween())
                                val edgeTarget = if (direction == Gravity.START)
                                    -deltaX.coerceIn(0f, edgeSlop.toFloat())
                                else (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                                edgeOffset.animateTo(edgeTarget, tween())
                            }
                        },
                        state = rememberDraggableState {
                            scope.launch {
                                deltaX += it
                                Log.i("TAG", "FloatingContentaaa: $deltaX")
                                val b = if (direction == Gravity.START && deltaX >= 0) {
                                    deltaX - it <= edgeSlop
                                } else if (direction == Gravity.END && deltaX <= 0) {
                                    deltaX - it >= -edgeSlop
                                } else false
                                if (b) {
                                    val edgeTarget = if (direction == Gravity.START)
                                        -deltaX.coerceIn(0f, edgeSlop.toFloat())
                                    else (-deltaX).coerceIn(0f, edgeSlop.toFloat())
                                    edgeOffset.animateTo(edgeTarget)
                                } else {
                                    val target = if (direction == Gravity.START)
                                        (-contentWidthPx + deltaX).coerceIn(-contentWidthPx, 0f)
                                    else (contentWidthPx + deltaX).coerceIn(0f, contentWidthPx)
                                    animOffset.animateTo(target)
                                }
                            }
                        }
                    ))


//            val deltaXTransform by remember {
//                derivedStateOf {
//                    Log.i("TAG", "FloatingContentbbb: deltaX $deltaX  direction $direction")
//                    if (direction == Gravity.START) {
//                        if (deltaX <= edgeSlop) {
//                            return@derivedStateOf -contentWidthPx
//                        }
//                        (-contentWidthPx + (deltaX - edgeSlop)).coerceAtMost(0f)
//                    } else {
//                        if (deltaX >= -edgeSlop) {
//                            return@derivedStateOf contentWidthPx
//                        }
//                        (contentWidthPx + (deltaX + edgeSlop)).coerceAtLeast(0f)
//                    }
//                }
//            }

//            val animOffset by animateIntOffsetAsState(IntOffset(deltaXTransform.toInt(), 0)) {
//                contentState = if (direction == Gravity.START) {
//                    it.x != -contentWidthPx.toInt()
//                } else {
//                    it.x != contentWidthPx.toInt()
//                }
//            }

            // 内容
            val pagerState = rememberPagerState(pageCount = { 4 })
            val measureResults: MutableMap<Boolean, MeasureResult> = remember { mutableMapOf() }
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    // 通过 layout 动态裁剪布局边界
                    .layout { measurable, constraints ->
                        // 计算实际可见区域
                        val visibleWidth = if (direction == Gravity.START) {
                            (contentWidthPx + animOffset.value) > 0
                        } else {
                            (contentWidthPx - animOffset.value) > 0
                        }
                        Log.i("TAG", "FloatingContent: contentWidthPx $visibleWidth")
                        measureResults.getOrPut(visibleWidth) {
                            val placeable = measurable.measure(constraints)
                            layout(if (visibleWidth) placeable.width else 0, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                    }
                    .graphicsLayer {
                        translationX = animOffset.value
                        alpha = 1f - (animOffset.value.absoluteValue / contentWidthPx)
                        Log.i("TAG", "FloatingContent: contentWidthPx $contentWidthPx  ${animOffset.value}")
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
            (if (interactions.isNotEmpty()) edgeSlop else edgeSlop / 2).toDp()
        })

        Box(
            modifier = modifier
                .width(widthAnima)
                .height(100.dp)
                .background(if (enableDrag) ColorEdgeEdit else Background, shape = RoundedCornerShape(percent = 50))
        )
    }
}



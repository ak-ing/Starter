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
import com.aking.starter.utils.dpValue
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * @author Ak
 * 2025/3/21  9:12
 */
class StarterFloatingComposeView(context: Context) : BaseFloatingComposeView(context) {

    override suspend fun handlerEdgeState() {
        if (edgeState) {
            deltaX = -1f
            dragTo(isRelease = true)
        } else {
            deltaX = contentWidthPx
            dragTo(isRelease = true)
        }
    }

    // 记住交互事件（松手时需要移除）
    private val pressInteraction = PressInteraction.Press(Offset.Zero)
    private val contentWidthPx = 150.dpValue + edgeSlop * 2

    // 滑动距离
    private var deltaX = 0f

    // 内容是否显示
    var visibleContent = false

    // EdgeBar偏移动画
    val edgeOffset = Animatable(0f)

    //内容偏移动画
    val animOffset = Animatable(-contentWidthPx)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun FloatingContent() {
        Box {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            // 拖动状态（长按拖拽）
            val dragState by interactionSource.collectIsDraggedAsState()

            LaunchedEffect(direction) {
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
                                    expand()
                                } else {
                                    shrink()
                                }
                            }
                        },
                        state = rememberDraggableState {
                            scope.launch { dragTo(delta = it) }
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
                            (contentWidthPx + animOffset.value) > 0 && !dragState
                        } else {
                            (contentWidthPx - animOffset.value) > 0 && !dragState
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


